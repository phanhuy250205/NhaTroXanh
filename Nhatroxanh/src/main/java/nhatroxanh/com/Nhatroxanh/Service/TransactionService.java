package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Transaction;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionService {

    /**
     * Tạo yêu cầu nạp tiền
     */
    Transaction createDepositRequest(Users user, Double amount, String paymentMethod, String description);

    /**
     * Tạo yêu cầu rút tiền
     */
    Transaction createWithdrawalRequest(Users user, Double amount, String bankName, 
                                      String accountNumber, String accountHolderName, String description);

    /**
     * Xác thực mật khẩu người dùng
     */
    boolean verifyUserPassword(Users user, String password);

    /**
     * Tạo yêu cầu rút tiền với xác thực OTP
     */
    Transaction createWithdrawalRequestWithOtp(Users user, Double amount, String bankName, 
                                             String accountNumber, String accountHolderName, String description);

    /**
     * Duyệt giao dịch
     */
    Transaction approveTransaction(Integer transactionId, Users approvedBy, String approvalNote);

    /**
     * Từ chối giao dịch
     */
    Transaction rejectTransaction(Integer transactionId, Users approvedBy, String rejectionReason);

    /**
     * Hoàn thành giao dịch (cập nhật số dư)
     */
    Transaction completeTransaction(Integer transactionId);

    /**
     * Đánh dấu giao dịch thất bại
     */
    Transaction failTransaction(Integer transactionId, String reason);

    /**
     * Lấy giao dịch theo ID
     */
    Optional<Transaction> getTransactionById(Integer transactionId);

    /**
     * Lấy danh sách giao dịch của user
     */
    List<Transaction> getTransactionsByUser(Users user);
    
    Page<Transaction> getTransactionsByUser(Users user, Pageable pageable);

    /**
     * Lấy danh sách giao dịch theo loại của user
     */
    List<Transaction> getTransactionsByUserAndType(Users user, Transaction.TransactionType type);
    
    Page<Transaction> getTransactionsByUserAndType(Users user, Transaction.TransactionType type, Pageable pageable);

    /**
     * Lấy danh sách giao dịch chờ duyệt
     */
    List<Transaction> getPendingTransactions();
    
    Page<Transaction> getPendingTransactions(Pageable pageable);

    /**
     * Lấy danh sách giao dịch chờ duyệt theo loại
     */
    List<Transaction> getPendingTransactionsByType(Transaction.TransactionType type);
    
    Page<Transaction> getPendingTransactionsByType(Transaction.TransactionType type, Pageable pageable);

    /**
     * Lấy thống kê giao dịch của user
     */
    TransactionStats getTransactionStats(Users user);

    /**
     * Tìm kiếm giao dịch
     */
    Page<Transaction> searchTransactions(Integer userId, Transaction.TransactionStatus status,
                                       Transaction.TransactionType type, String keyword, Pageable pageable);

    /**
     * Lấy giao dịch trong khoảng thời gian
     */
    List<Transaction> getTransactionsByDateRange(Users user, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Đếm số giao dịch chờ duyệt
     */
    Long countPendingTransactions();
    
    Long countPendingTransactionsByType(Transaction.TransactionType type);

    /**
     * Lấy giao dịch được duyệt bởi staff
     */
    List<Transaction> getTransactionsApprovedBy(Users staff);
    
    Page<Transaction> getTransactionsApprovedBy(Users staff, Pageable pageable);

    /**
     * Class cho thống kê giao dịch
     */
    class TransactionStats {
        private Long totalTransactions;
        private Long pendingTransactions;
        private Long completedTransactions;
        private Long rejectedTransactions;
        private Double totalDeposited;
        private Double totalWithdrawn;
        private Double currentBalance;

        // Constructors
        public TransactionStats() {}

        public TransactionStats(Long totalTransactions, Long pendingTransactions, 
                              Long completedTransactions, Long rejectedTransactions,
                              Double totalDeposited, Double totalWithdrawn, Double currentBalance) {
            this.totalTransactions = totalTransactions;
            this.pendingTransactions = pendingTransactions;
            this.completedTransactions = completedTransactions;
            this.rejectedTransactions = rejectedTransactions;
            this.totalDeposited = totalDeposited;
            this.totalWithdrawn = totalWithdrawn;
            this.currentBalance = currentBalance;
        }

        // Getters and Setters
        public Long getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(Long totalTransactions) { this.totalTransactions = totalTransactions; }

        public Long getPendingTransactions() { return pendingTransactions; }
        public void setPendingTransactions(Long pendingTransactions) { this.pendingTransactions = pendingTransactions; }

        public Long getCompletedTransactions() { return completedTransactions; }
        public void setCompletedTransactions(Long completedTransactions) { this.completedTransactions = completedTransactions; }

        public Long getRejectedTransactions() { return rejectedTransactions; }
        public void setRejectedTransactions(Long rejectedTransactions) { this.rejectedTransactions = rejectedTransactions; }

        public Double getTotalDeposited() { return totalDeposited != null ? totalDeposited : 0.0; }
        public void setTotalDeposited(Double totalDeposited) { this.totalDeposited = totalDeposited; }

        public Double getTotalWithdrawn() { return totalWithdrawn != null ? totalWithdrawn : 0.0; }
        public void setTotalWithdrawn(Double totalWithdrawn) { this.totalWithdrawn = totalWithdrawn; }

        public Double getCurrentBalance() { return currentBalance != null ? currentBalance : 0.0; }
        public void setCurrentBalance(Double currentBalance) { this.currentBalance = currentBalance; }
    }
}
