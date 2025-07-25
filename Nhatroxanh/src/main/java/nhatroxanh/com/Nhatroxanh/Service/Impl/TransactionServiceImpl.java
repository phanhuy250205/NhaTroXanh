package nhatroxanh.com.Nhatroxanh.Service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.Transaction;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.TransactionRepository;
import nhatroxanh.com.Nhatroxanh.Service.TransactionService;
import nhatroxanh.com.Nhatroxanh.Service.WalletService;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Transaction createDepositRequest(Users user, Double amount, String paymentMethod, String description) {
        log.info("Creating deposit request for user {} with amount {}", user.getUserId(), amount);
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .status(Transaction.TransactionStatus.PENDING)
                .paymentMethod(paymentMethod)
                .description(description != null ? description : "Yêu cầu nạp tiền")
                .transactionReference(generateTransactionReference("DEP"))
                .createdAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created deposit request with ID: {}", savedTransaction.getTransactionId());
        
        return savedTransaction;
    }

    @Override
    @Transactional
    public Transaction createWithdrawalRequest(Users user, Double amount, String bankName, 
                                             String accountNumber, String accountHolderName, String description) {
        log.info("Creating withdrawal request for user {} with amount {}", user.getUserId(), amount);
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0");
        }

        // Kiểm tra số dư
        if (!walletService.hasSufficientBalance(user.getUserId(), amount)) {
            throw new IllegalArgumentException("Số dư tài khoản không đủ để thực hiện giao dịch");
        }

        // Trừ số dư ngay khi chủ trọ xác nhận rút tiền
        try {
            walletService.subtractBalance(user, amount, "Rút tiền - " + (description != null ? description : "Yêu cầu rút tiền"));
            log.info("Deducted {} from user {} balance for withdrawal request", amount, user.getUserId());
        } catch (Exception e) {
            log.error("Error deducting balance for withdrawal request: {}", e.getMessage());
            throw new RuntimeException("Không thể trừ số dư: " + e.getMessage());
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .amount(amount)
                .status(Transaction.TransactionStatus.PENDING)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .description(description != null ? description : "Yêu cầu rút tiền")
                .transactionReference(generateTransactionReference("WTH"))
                .createdAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created withdrawal request with ID: {} and deducted balance", savedTransaction.getTransactionId());
        
        return savedTransaction;
    }

    @Override
    @Transactional
    public Transaction approveTransaction(Integer transactionId, Users approvedBy, String approvalNote) {
        log.info("Approving transaction {} by user {}", transactionId, approvedBy.getUserId());
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch với ID: " + transactionId));

        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể duyệt giao dịch đang chờ xử lý");
        }

        transaction.setStatus(Transaction.TransactionStatus.APPROVED);
        transaction.setApprovedBy(approvedBy);
        transaction.setApprovalNote(approvalNote);
        transaction.setProcessedAt(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Approved transaction {} successfully", transactionId);
        
        return savedTransaction;
    }

    @Override
    @Transactional
    public Transaction rejectTransaction(Integer transactionId, Users approvedBy, String rejectionReason) {
        log.info("Rejecting transaction {} by user {}", transactionId, approvedBy.getUserId());
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch với ID: " + transactionId));

        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể từ chối giao dịch đang chờ xử lý");
        }

        // Nếu là giao dịch rút tiền bị từ chối, trả lại số dư cho người dùng
        if (transaction.getTransactionType() == Transaction.TransactionType.WITHDRAWAL) {
            try {
                walletService.addBalance(transaction.getUser(), transaction.getAmount(), 
                    "Hoàn trả do rút tiền bị từ chối - " + rejectionReason);
                log.info("Returned {} to user {} balance due to withdrawal rejection", 
                    transaction.getAmount(), transaction.getUser().getUserId());
            } catch (Exception e) {
                log.error("Error returning balance for rejected withdrawal transaction {}: {}", 
                    transactionId, e.getMessage());
                throw new RuntimeException("Không thể hoàn trả số dư: " + e.getMessage());
            }
        }

        transaction.setStatus(Transaction.TransactionStatus.REJECTED);
        transaction.setApprovedBy(approvedBy);
        transaction.setRejectionReason(rejectionReason);
        transaction.setProcessedAt(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Rejected transaction {} successfully and returned balance if applicable", transactionId);
        
        return savedTransaction;
    }

    @Override
    @Transactional
    public Transaction completeTransaction(Integer transactionId) {
        log.info("Completing transaction {}", transactionId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch với ID: " + transactionId));

        if (transaction.getStatus() != Transaction.TransactionStatus.APPROVED) {
            throw new IllegalStateException("Chỉ có thể hoàn thành giao dịch đã được duyệt");
        }

        try {
            // Cập nhật số dư - chỉ cho giao dịch nạp tiền
            // Giao dịch rút tiền đã được trừ số dư khi tạo yêu cầu
            if (transaction.getTransactionType() == Transaction.TransactionType.DEPOSIT) {
                walletService.addBalance(transaction.getUser(), transaction.getAmount(), 
                    "Nạp tiền - " + transaction.getDescription());
                log.info("Added {} to user {} balance for completed deposit transaction {}", 
                    transaction.getAmount(), transaction.getUser().getUserId(), transactionId);
            } else if (transaction.getTransactionType() == Transaction.TransactionType.WITHDRAWAL) {
                // Không trừ số dư vì đã trừ khi tạo yêu cầu rút tiền
                log.info("Withdrawal transaction {} completed - balance was already deducted when request was created", 
                    transactionId);
            }

            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            Transaction savedTransaction = transactionRepository.save(transaction);
            
            log.info("Completed transaction {} successfully", transactionId);
            return savedTransaction;
            
        } catch (Exception e) {
            log.error("Error completing transaction {}: {}", transactionId, e.getMessage());
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new RuntimeException("Lỗi khi hoàn thành giao dịch: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Transaction failTransaction(Integer transactionId, String reason) {
        log.info("Failing transaction {} with reason: {}", transactionId, reason);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch với ID: " + transactionId));

        transaction.setStatus(Transaction.TransactionStatus.FAILED);
        transaction.setRejectionReason(reason);
        transaction.setProcessedAt(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Failed transaction {} successfully", transactionId);
        
        return savedTransaction;
    }

    @Override
    public Optional<Transaction> getTransactionById(Integer transactionId) {
        return transactionRepository.findById(transactionId);
    }

    @Override
    public List<Transaction> getTransactionsByUser(Users user) {
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public Page<Transaction> getTransactionsByUser(Users user, Pageable pageable) {
        return transactionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Override
    public List<Transaction> getTransactionsByUserAndType(Users user, Transaction.TransactionType type) {
        return transactionRepository.findByUserAndTransactionTypeOrderByCreatedAtDesc(user, type);
    }

    @Override
    public Page<Transaction> getTransactionsByUserAndType(Users user, Transaction.TransactionType type, Pageable pageable) {
        return transactionRepository.findByUserAndTransactionTypeOrderByCreatedAtDesc(user, type, pageable);
    }

    @Override
    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findPendingTransactions();
    }

    @Override
    public Page<Transaction> getPendingTransactions(Pageable pageable) {
        return transactionRepository.findPendingTransactions(pageable);
    }

    @Override
    public List<Transaction> getPendingTransactionsByType(Transaction.TransactionType type) {
        return transactionRepository.findPendingTransactionsByType(type);
    }

    @Override
    public Page<Transaction> getPendingTransactionsByType(Transaction.TransactionType type, Pageable pageable) {
        return transactionRepository.findPendingTransactionsByType(type, pageable);
    }

    @Override
    public TransactionStats getTransactionStats(Users user) {
        Long totalTransactions = (long) transactionRepository.findByUserOrderByCreatedAtDesc(user).size();
        Long pendingTransactions = transactionRepository.countByUserAndStatus(user, Transaction.TransactionStatus.PENDING);
        Long completedTransactions = transactionRepository.countByUserAndStatus(user, Transaction.TransactionStatus.COMPLETED);
        Long rejectedTransactions = transactionRepository.countByUserAndStatus(user, Transaction.TransactionStatus.REJECTED);
        
        Double totalDeposited = transactionRepository.sumAmountByUserAndTypeAndCompleted(user, Transaction.TransactionType.DEPOSIT);
        Double totalWithdrawn = transactionRepository.sumAmountByUserAndTypeAndCompleted(user, Transaction.TransactionType.WITHDRAWAL);
        Double currentBalance = walletService.getBalance(user.getUserId());

        return new TransactionStats(totalTransactions, pendingTransactions, completedTransactions, 
                                  rejectedTransactions, totalDeposited, totalWithdrawn, currentBalance);
    }

    @Override
    public Page<Transaction> searchTransactions(Integer userId, Transaction.TransactionStatus status,
                                              Transaction.TransactionType type, String keyword, Pageable pageable) {
        return transactionRepository.searchTransactions(userId, status, type, keyword, pageable);
    }

    @Override
    public List<Transaction> getTransactionsByDateRange(Users user, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByUserAndDateRange(user, startDate, endDate);
    }

    @Override
    public Long countPendingTransactions() {
        return transactionRepository.countPendingTransactions();
    }

    @Override
    public Long countPendingTransactionsByType(Transaction.TransactionType type) {
        return transactionRepository.countPendingTransactionsByType(type);
    }

    @Override
    public List<Transaction> getTransactionsApprovedBy(Users staff) {
        return transactionRepository.findByApprovedByOrderByProcessedAtDesc(staff);
    }

    @Override
    public Page<Transaction> getTransactionsApprovedBy(Users staff, Pageable pageable) {
        return transactionRepository.findByApprovedByOrderByProcessedAtDesc(staff, pageable);
    }

    @Override
    public boolean verifyUserPassword(Users user, String password) {
        log.info("Verifying password for user {}", user.getUserId());
        
        if (user == null || password == null || password.trim().isEmpty()) {
            log.warn("Invalid user or password provided for verification");
            return false;
        }
        
        try {
            boolean isValid = passwordEncoder.matches(password, user.getPassword());
            log.info("Password verification for user {} result: {}", user.getUserId(), isValid);
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying password for user {}: {}", user.getUserId(), e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public Transaction createWithdrawalRequestWithOtp(Users user, Double amount, String bankName, 
                                                    String accountNumber, String accountHolderName, String description) {
        log.info("Creating withdrawal request with OTP for user {} with amount {}", user.getUserId(), amount);
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0");
        }

        // Kiểm tra số dư
        if (!walletService.hasSufficientBalance(user.getUserId(), amount)) {
            throw new IllegalArgumentException("Số dư tài khoản không đủ để thực hiện giao dịch");
        }

        // Tạo và gửi OTP cho giao dịch rút tiền
        try {
            otpService.createAndSendWithdrawalOtp(user, amount);
            log.info("OTP sent successfully for withdrawal request of user {}", user.getUserId());
        } catch (Exception e) {
            log.error("Error sending OTP for withdrawal request of user {}: {}", user.getUserId(), e.getMessage());
            throw new RuntimeException("Không thể gửi mã OTP. Vui lòng thử lại sau.");
        }

        // Trừ số dư ngay khi chủ trọ xác nhận rút tiền với OTP
        try {
            walletService.subtractBalance(user, amount, "Rút tiền với OTP - " + (description != null ? description : "Yêu cầu rút tiền với xác thực OTP"));
            log.info("Deducted {} from user {} balance for withdrawal request with OTP", amount, user.getUserId());
        } catch (Exception e) {
            log.error("Error deducting balance for withdrawal request with OTP: {}", e.getMessage());
            throw new RuntimeException("Không thể trừ số dư: " + e.getMessage());
        }

        // Tạo giao dịch với trạng thái chờ xác thực OTP
        Transaction transaction = Transaction.builder()
                .user(user)
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .amount(amount)
                .status(Transaction.TransactionStatus.PENDING)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .description(description != null ? description : "Yêu cầu rút tiền với xác thực OTP")
                .transactionReference(generateTransactionReference("WTH_OTP"))
                .createdAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created withdrawal request with OTP verification with ID: {} and deducted balance", savedTransaction.getTransactionId());
        
        return savedTransaction;
    }

    /**
     * Tạo mã tham chiếu giao dịch duy nhất
     */
    private String generateTransactionReference(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
