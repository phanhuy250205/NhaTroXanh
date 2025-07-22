package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.Transaction;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/staff/transactions")
@RequiredArgsConstructor
@Slf4j
public class StaffTransactionController {

    private final TransactionService transactionService;

    /**
     * Hiển thị trang duyệt nạp tiền và rút tiền
     */
    @GetMapping("/duyet-nap-rut")
    @Transactional(readOnly = true)
    public String showApprovalPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) String status,
                                 Model model) {
        try {
            Users user = userDetails.getUser();
            log.info("Staff {} accessing approval page with params: page={}, size={}, type={}, status={}", 
                user.getUserId(), page, size, type, status);
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.STAFF && user.getRole() != Users.Role.ADMIN) {
                log.warn("User {} with role {} attempted to access staff approval page", 
                    user.getUserId(), user.getRole());
                return "redirect:/access-denied";
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Transaction> transactions;

            // Lọc theo loại giao dịch và trạng thái
            if (type != null && !type.isEmpty()) {
                try {
                    Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(type.toUpperCase());
                    if ("PENDING".equals(status)) {
                        transactions = transactionService.getPendingTransactionsByType(transactionType, pageable);
                        log.info("Retrieved {} pending transactions of type {}", 
                            transactions.getTotalElements(), transactionType);
                    } else {
                        Transaction.TransactionStatus transactionStatus = null;
                        if (status != null && !status.isEmpty()) {
                            transactionStatus = Transaction.TransactionStatus.valueOf(status.toUpperCase());
                        }
                        transactions = transactionService.searchTransactions(null, 
                            transactionStatus, transactionType, null, pageable);
                        log.info("Retrieved {} transactions with type {} and status {}", 
                            transactions.getTotalElements(), transactionType, transactionStatus);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid transaction type or status: type={}, status={}", type, status);
                    transactions = transactionService.getPendingTransactions(pageable);
                }
            } else {
                if ("PENDING".equals(status) || status == null || status.isEmpty()) {
                    transactions = transactionService.getPendingTransactions(pageable);
                    log.info("Retrieved {} pending transactions", transactions.getTotalElements());
                } else {
                    try {
                        Transaction.TransactionStatus transactionStatus = Transaction.TransactionStatus.valueOf(status.toUpperCase());
                        transactions = transactionService.searchTransactions(null, 
                            transactionStatus, null, null, pageable);
                        log.info("Retrieved {} transactions with status {}", 
                            transactions.getTotalElements(), transactionStatus);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid transaction status: {}", status);
                        transactions = transactionService.getPendingTransactions(pageable);
                    }
                }
            }

            // Đảm bảo dữ liệu được load đầy đủ
            if (transactions != null && transactions.hasContent()) {
                // Force load lazy relationships
                transactions.getContent().forEach(transaction -> {
                    if (transaction.getUser() != null) {
                        transaction.getUser().getFullname(); // Force load user data
                        transaction.getUser().getPhone();
                        transaction.getUser().getEmail();
                    }
                    if (transaction.getApprovedBy() != null) {
                        transaction.getApprovedBy().getFullname(); // Force load approver data
                    }
                });
                log.info("Successfully loaded {} transactions with user data", transactions.getContent().size());
            }

            model.addAttribute("transactions", transactions);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", transactions != null ? transactions.getTotalPages() : 0);
            model.addAttribute("selectedType", type);
            model.addAttribute("selectedStatus", status);

            // Thống kê số lượng giao dịch chờ duyệt
            Long pendingDeposits = transactionService.countPendingTransactionsByType(Transaction.TransactionType.DEPOSIT);
            Long pendingWithdrawals = transactionService.countPendingTransactionsByType(Transaction.TransactionType.WITHDRAWAL);
            Long totalPending = transactionService.countPendingTransactions();

            // Đảm bảo không có giá trị null
            pendingDeposits = pendingDeposits != null ? pendingDeposits : 0L;
            pendingWithdrawals = pendingWithdrawals != null ? pendingWithdrawals : 0L;
            totalPending = totalPending != null ? totalPending : 0L;

            model.addAttribute("pendingDeposits", pendingDeposits);
            model.addAttribute("pendingWithdrawals", pendingWithdrawals);
            model.addAttribute("totalPending", totalPending);

            log.info("Successfully loaded approval page data: totalPending={}, pendingDeposits={}, pendingWithdrawals={}", 
                totalPending, pendingDeposits, pendingWithdrawals);

            return "staff/duyet-nap-rut";
        } catch (Exception e) {
            log.error("Error showing approval page for staff {}: {}", 
                userDetails != null ? userDetails.getUserId() : "unknown", e.getMessage(), e);
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
            return "staff/duyet-nap-rut";
        }
    }

    /**
     * Duyệt giao dịch
     */
    @PostMapping("/approve/{transactionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer transactionId,
            @RequestParam(required = false) String approvalNote) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users staff = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (staff.getRole() != Users.Role.STAFF && staff.getRole() != Users.Role.ADMIN) {
                response.put("success", false);
                response.put("message", "Không có quyền thực hiện thao tác này");
                return ResponseEntity.badRequest().body(response);
            }

            // Duyệt giao dịch
            Transaction transaction = transactionService.approveTransaction(transactionId, staff, approvalNote);
            
            // Hoàn thành giao dịch (cập nhật số dư)
            // Đối với giao dịch rút tiền: số dư đã được trừ khi tạo yêu cầu, chỉ cần đánh dấu hoàn thành
            // Đối với giao dịch nạp tiền: cần cộng số dư khi hoàn thành
            transactionService.completeTransaction(transactionId);

            response.put("success", true);
            response.put("message", "Đã duyệt giao dịch thành công");
            response.put("transactionId", transaction.getTransactionId());
            response.put("transactionType", transaction.getTransactionType().toString());

            log.info("Staff {} approved and completed transaction {} successfully", 
                staff.getUserId(), transactionId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid approval request from staff {}: {}", userDetails.getUserId(), e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error approving transaction {} by staff {}: {}", 
                transactionId, userDetails.getUserId(), e.getMessage());
            
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi duyệt giao dịch: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Từ chối giao dịch
     */
    @PostMapping("/reject/{transactionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer transactionId,
            @RequestParam String rejectionReason) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users staff = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (staff.getRole() != Users.Role.STAFF && staff.getRole() != Users.Role.ADMIN) {
                response.put("success", false);
                response.put("message", "Không có quyền thực hiện thao tác này");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate lý do từ chối
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập lý do từ chối");
                return ResponseEntity.badRequest().body(response);
            }

            // Từ chối giao dịch
            Transaction transaction = transactionService.rejectTransaction(transactionId, staff, rejectionReason);

            response.put("success", true);
            response.put("message", "Đã từ chối giao dịch thành công");
            response.put("transactionId", transaction.getTransactionId());
            response.put("transactionType", transaction.getTransactionType().toString());

            log.info("Staff {} rejected transaction {} with reason: {}", 
                staff.getUserId(), transactionId, rejectionReason);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid rejection request from staff {}: {}", userDetails.getUserId(), e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error rejecting transaction {} by staff {}: {}", 
                transactionId, userDetails.getUserId(), e.getMessage());
            
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi từ chối giao dịch: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xem chi tiết giao dịch
     */
    @GetMapping("/detail/{transactionId}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getTransactionDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer transactionId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users staff = userDetails.getUser();
            log.info("Staff {} requesting transaction detail for ID: {}", staff.getUserId(), transactionId);
            
            // Kiểm tra quyền truy cập
            if (staff.getRole() != Users.Role.STAFF && staff.getRole() != Users.Role.ADMIN) {
                log.warn("User {} with role {} attempted to access transaction detail", 
                    staff.getUserId(), staff.getRole());
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            Transaction transaction = transactionService.getTransactionById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch với ID: " + transactionId));

            // Force load lazy relationships to avoid serialization issues
            if (transaction.getUser() != null) {
                transaction.getUser().getFullname();
                transaction.getUser().getPhone();
                transaction.getUser().getEmail();
                log.info("Loaded user data for transaction {}: {}", transactionId, transaction.getUser().getFullname());
            }
            
            if (transaction.getApprovedBy() != null) {
                transaction.getApprovedBy().getFullname();
                log.info("Loaded approver data for transaction {}: {}", transactionId, transaction.getApprovedBy().getFullname());
            }

            response.put("success", true);
            response.put("transaction", transaction);
            
            log.info("Successfully retrieved transaction detail for ID: {}", transactionId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Transaction not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error getting transaction detail {} by staff {}: {}", 
                transactionId, userDetails != null ? userDetails.getUserId() : "unknown", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Không thể lấy thông tin giao dịch: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Tìm kiếm giao dịch
     */
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public String searchTransactions(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {
        try {
            Users staff = userDetails.getUser();
            log.info("Staff {} searching transactions with keyword={}, type={}, status={}", 
                staff.getUserId(), keyword, type, status);
            
            // Kiểm tra quyền truy cập
            if (staff.getRole() != Users.Role.STAFF && staff.getRole() != Users.Role.ADMIN) {
                log.warn("User {} with role {} attempted to search transactions", 
                    staff.getUserId(), staff.getRole());
                return "redirect:/access-denied";
            }

            Pageable pageable = PageRequest.of(page, size);
            
            Transaction.TransactionType transactionType = null;
            if (type != null && !type.isEmpty()) {
                try {
                    transactionType = Transaction.TransactionType.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid transaction type: {}", type);
                }
            }
            
            Transaction.TransactionStatus transactionStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    transactionStatus = Transaction.TransactionStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid transaction status: {}", status);
                }
            }

            Page<Transaction> transactions = transactionService.searchTransactions(
                null, transactionStatus, transactionType, keyword, pageable);

            // Force load lazy relationships
            if (transactions != null && transactions.hasContent()) {
                transactions.getContent().forEach(transaction -> {
                    if (transaction.getUser() != null) {
                        transaction.getUser().getFullname();
                        transaction.getUser().getPhone();
                        transaction.getUser().getEmail();
                    }
                    if (transaction.getApprovedBy() != null) {
                        transaction.getApprovedBy().getFullname();
                    }
                });
                log.info("Search returned {} transactions", transactions.getContent().size());
            }

            model.addAttribute("transactions", transactions);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", transactions != null ? transactions.getTotalPages() : 0);
            model.addAttribute("keyword", keyword);
            model.addAttribute("selectedType", type);
            model.addAttribute("selectedStatus", status);

            // Thống kê
            Long pendingDeposits = transactionService.countPendingTransactionsByType(Transaction.TransactionType.DEPOSIT);
            Long pendingWithdrawals = transactionService.countPendingTransactionsByType(Transaction.TransactionType.WITHDRAWAL);
            Long totalPending = transactionService.countPendingTransactions();

            // Đảm bảo không có giá trị null
            pendingDeposits = pendingDeposits != null ? pendingDeposits : 0L;
            pendingWithdrawals = pendingWithdrawals != null ? pendingWithdrawals : 0L;
            totalPending = totalPending != null ? totalPending : 0L;

            model.addAttribute("pendingDeposits", pendingDeposits);
            model.addAttribute("pendingWithdrawals", pendingWithdrawals);
            model.addAttribute("totalPending", totalPending);

            return "staff/duyet-nap-rut";
        } catch (Exception e) {
            log.error("Error searching transactions by staff {}: {}", 
                userDetails != null ? userDetails.getUserId() : "unknown", e.getMessage(), e);
            model.addAttribute("error", "Có lỗi xảy ra khi tìm kiếm: " + e.getMessage());
            return "staff/duyet-nap-rut";
        }
    }

    /**
     * API lấy thống kê cho staff
     */
    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStaffStats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users staff = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (staff.getRole() != Users.Role.STAFF && staff.getRole() != Users.Role.ADMIN) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            Long pendingDeposits = transactionService.countPendingTransactionsByType(Transaction.TransactionType.DEPOSIT);
            Long pendingWithdrawals = transactionService.countPendingTransactionsByType(Transaction.TransactionType.WITHDRAWAL);
            Long totalPending = transactionService.countPendingTransactions();

            // Lấy giao dịch đã duyệt bởi staff này
            Long approvedByStaff = (long) transactionService.getTransactionsApprovedBy(staff).size();

            Map<String, Object> stats = new HashMap<>();
            stats.put("pendingDeposits", pendingDeposits);
            stats.put("pendingWithdrawals", pendingWithdrawals);
            stats.put("totalPending", totalPending);
            stats.put("approvedByStaff", approvedByStaff);

            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting staff stats for user {}: {}", userDetails.getUserId(), e.getMessage());
            response.put("success", false);
            response.put("message", "Không thể lấy thống kê");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
