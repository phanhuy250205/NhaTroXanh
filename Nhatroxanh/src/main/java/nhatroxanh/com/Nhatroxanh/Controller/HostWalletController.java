package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.Transaction;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.TransactionService;
import nhatroxanh.com.Nhatroxanh.Service.WalletService;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.WalletPaymentService;
import nhatroxanh.com.Nhatroxanh.Service.VNPayService;
import nhatroxanh.com.Nhatroxanh.Service.MoMoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;

@Controller
@RequestMapping("/host/wallet")
@RequiredArgsConstructor
@Slf4j
public class HostWalletController {

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final OtpService otpService;
    private final WalletPaymentService walletPaymentService;


    // VNPay Configuration for signature verification
    @Value("${vnpay.hashSecret:DEMO}")
    private String vnpHashSecret;

    /**
     * Hiển thị trang nạp tiền
     */
    @GetMapping("/nap-tien")
    public String showDepositPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                return "redirect:/access-denied";
            }

            // Lấy số dư hiện tại
            Double currentBalance = walletService.getBalance(user.getUserId());
            model.addAttribute("currentBalance", currentBalance);

            // Lấy thống kê giao dịch
            TransactionService.TransactionStats stats = transactionService.getTransactionStats(user);
            model.addAttribute("transactionStats", stats);

            // Lấy lịch sử giao dịch nạp tiền gần đây
            Pageable pageable = PageRequest.of(0, 5);
            Page<Transaction> recentDeposits = transactionService.getTransactionsByUserAndType(
                user, Transaction.TransactionType.DEPOSIT, pageable);
            model.addAttribute("recentDeposits", recentDeposits.getContent());

            return "host/nap-tien";
        } catch (Exception e) {
            log.error("Error showing deposit page for user {}: {}", userDetails.getUserId(), e.getMessage());
            return "redirect:/host/dashboard?error=system_error";
        }
    }

    /**
     * Xử lý yêu cầu nạp tiền
     */
    @PostMapping("/nap-tien")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processDepositRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("amount") Double amount,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "description", required = false) String description) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate input
            if (amount == null || amount <= 0) {
                response.put("success", false);
                response.put("message", "Số tiền nạp phải lớn hơn 0");
                return ResponseEntity.badRequest().body(response);
            }

            if (amount < 50000) {
                response.put("success", false);
                response.put("message", "Số tiền nạp tối thiểu là 50,000 VNĐ");
                return ResponseEntity.badRequest().body(response);
            }

            if (amount > 50000000) {
                response.put("success", false);
                response.put("message", "Số tiền nạp tối đa là 50,000,000 VNĐ");
                return ResponseEntity.badRequest().body(response);
            }

            // Tạo yêu cầu nạp tiền
            Transaction transaction = transactionService.createDepositRequest(
                user, amount, paymentMethod, description);

            response.put("success", true);
            response.put("message", "Yêu cầu nạp tiền đã được gửi thành công. Vui lòng chờ staff duyệt.");
            response.put("transactionId", transaction.getTransactionId());
            response.put("transactionReference", transaction.getTransactionReference());

            log.info("Created deposit request {} for user {} with amount {}", 
                transaction.getTransactionId(), user.getUserId(), amount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing deposit request for user {}: {}", 
                userDetails.getUserId(), e.getMessage());
            
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xử lý yêu cầu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hiển thị trang rút tiền
     */
    @GetMapping("/rut-tien")
    public String showWithdrawalPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                return "redirect:/access-denied";
            }

            // Lấy số dư hiện tại
            Double currentBalance = walletService.getBalance(user.getUserId());
            model.addAttribute("currentBalance", currentBalance);

            // Lấy thống kê giao dịch
            TransactionService.TransactionStats stats = transactionService.getTransactionStats(user);
            model.addAttribute("transactionStats", stats);

            // Lấy lịch sử giao dịch rút tiền gần đây
            Pageable pageable = PageRequest.of(0, 5);
            Page<Transaction> recentWithdrawals = transactionService.getTransactionsByUserAndType(
                user, Transaction.TransactionType.WITHDRAWAL, pageable);
            model.addAttribute("recentWithdrawals", recentWithdrawals.getContent());

            return "host/rut-tien";
        } catch (Exception e) {
            log.error("Error showing withdrawal page for user {}: {}", userDetails.getUserId(), e.getMessage());
            return "redirect:/host/dashboard?error=system_error";
        }
    }

    /**
     * Xử lý yêu cầu rút tiền
     */
    @PostMapping("/rut-tien")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processWithdrawalRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("amount") Double amount,
            @RequestParam("bankName") String bankName,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("accountHolderName") String accountHolderName,
            @RequestParam(value = "description", required = false) String description) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate input
            if (amount == null || amount <= 0) {
                response.put("success", false);
                response.put("message", "Số tiền rút phải lớn hơn 0");
                return ResponseEntity.badRequest().body(response);
            }

            if (amount < 100000) {
                response.put("success", false);
                response.put("message", "Số tiền rút tối thiểu là 100,000 VNĐ");
                return ResponseEntity.badRequest().body(response);
            }

            if (amount > 10000000) {
                response.put("success", false);
                response.put("message", "Số tiền rút tối đa là 10,000,000 VNĐ mỗi lần");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra thông tin ngân hàng
            if (bankName == null || bankName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ngân hàng");
                return ResponseEntity.badRequest().body(response);
            }

            if (accountNumber == null || accountNumber.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập số tài khoản");
                return ResponseEntity.badRequest().body(response);
            }

            if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập tên chủ tài khoản");
                return ResponseEntity.badRequest().body(response);
            }

            // Tạo yêu cầu rút tiền
            Transaction transaction = transactionService.createWithdrawalRequest(
                user, amount, bankName, accountNumber, accountHolderName, description);

            response.put("success", true);
            response.put("message", "Yêu cầu rút tiền đã được gửi thành công. Vui lòng chờ staff duyệt.");
            response.put("transactionId", transaction.getTransactionId());
            response.put("transactionReference", transaction.getTransactionReference());

            log.info("Created withdrawal request {} for user {} with amount {}", 
                transaction.getTransactionId(), user.getUserId(), amount);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid withdrawal request from user {}: {}", userDetails.getUserId(), e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error processing withdrawal request for user {}: {}", 
                userDetails.getUserId(), e.getMessage());
            
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xử lý yêu cầu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy lịch sử giao dịch
     */
    @GetMapping("/lich-su")
    public String showTransactionHistory(@AuthenticationPrincipal CustomUserDetails userDetails, 
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String status,
                                       Model model) {
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                return "redirect:/access-denied";
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Transaction> transactions;

            // Lọc theo loại giao dịch nếu có
            if (type != null && !type.isEmpty()) {
                Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(type.toUpperCase());
                transactions = transactionService.getTransactionsByUserAndType(user, transactionType, pageable);
            } else {
                transactions = transactionService.getTransactionsByUser(user, pageable);
            }

            model.addAttribute("transactions", transactions);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", transactions.getTotalPages());
            model.addAttribute("selectedType", type);
            model.addAttribute("selectedStatus", status);

            // Lấy số dư hiện tại
            Double currentBalance = walletService.getBalance(user.getUserId());
            model.addAttribute("currentBalance", currentBalance);

            // Lấy thống kê
            TransactionService.TransactionStats stats = transactionService.getTransactionStats(user);
            model.addAttribute("transactionStats", stats);

            return "host/lich-su-giao-dich";
        } catch (Exception e) {
            log.error("Error showing transaction history for user {}: {}", userDetails.getUserId(), e.getMessage());
            return "redirect:/host/dashboard?error=system_error";
        }
    }

    /**
     * API lấy số dư hiện tại
     */
    @GetMapping("/balance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentBalance(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            Double balance = walletService.getBalance(user.getUserId());
            
            response.put("success", true);
            response.put("balance", balance);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting balance for user {}: {}", userDetails.getUserId(), e.getMessage());
            response.put("success", false);
            response.put("message", "Không thể lấy thông tin số dư");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API lấy thống kê giao dịch
     */
    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTransactionStats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            TransactionService.TransactionStats stats = transactionService.getTransactionStats(user);
            
            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting transaction stats for user {}: {}", userDetails.getUserId(), e.getMessage());
            response.put("success", false);
            response.put("message", "Không thể lấy thống kê giao dịch");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API xác thực mật khẩu người dùng
     */
    @PostMapping("/verify-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("password") String password) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate input
            if (password == null || password.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập mật khẩu");
                return ResponseEntity.badRequest().body(response);
            }

            // Xác thực mật khẩu
            boolean isValid = transactionService.verifyUserPassword(user, password);
            
            if (isValid) {
                response.put("success", true);
                response.put("message", "Xác thực mật khẩu thành công");
                log.info("Password verification successful for user {}", user.getUserId());
            } else {
                response.put("success", false);
                response.put("message", "Mật khẩu không đúng");
                log.warn("Password verification failed for user {}", user.getUserId());
            }
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying password for user {}: {}", userDetails.getUserId(), e.getMessage());
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xác thực mật khẩu");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API gửi OTP cho giao dịch rút tiền
     */
    @PostMapping("/send-withdrawal-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendWithdrawalOtp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("amount") Double amount) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate input
            if (amount == null || amount <= 0) {
                response.put("success", false);
                response.put("message", "Số tiền không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra số dư
            if (!walletService.hasSufficientBalance(user.getUserId(), amount)) {
                response.put("success", false);
                response.put("message", "Số dư tài khoản không đủ để thực hiện giao dịch");
                return ResponseEntity.badRequest().body(response);
            }

            // Gửi OTP
            otpService.createAndSendWithdrawalOtp(user, amount);
            
            response.put("success", true);
            response.put("message", "Mã OTP đã được gửi đến email của bạn");
            
            log.info("Withdrawal OTP sent successfully for user {} with amount {}", user.getUserId(), amount);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending withdrawal OTP for user {}: {}", userDetails.getUserId(), e.getMessage());
            response.put("success", false);
            response.put("message", "Không thể gửi mã OTP. Vui lòng thử lại sau.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API xác thực OTP và hoàn thành giao dịch rút tiền
     */
    @PostMapping("/verify-withdrawal-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyWithdrawalOtp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("otp") String otp,
            @RequestParam("amount") Double amount,
            @RequestParam("bankName") String bankName,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("accountHolderName") String accountHolderName,
            @RequestParam(value = "description", required = false) String description) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate input
            if (otp == null || otp.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập mã OTP");
                return ResponseEntity.badRequest().body(response);
            }

            if (amount == null || amount <= 0) {
                response.put("success", false);
                response.put("message", "Số tiền không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }

            // Xác thực OTP
            boolean isOtpValid = otpService.verifyWithdrawalOtp(user, otp);
            
            if (!isOtpValid) {
                response.put("success", false);
                response.put("message", "Mã OTP không đúng hoặc đã hết hạn");
                log.warn("Invalid OTP verification for withdrawal request from user {}", user.getUserId());
                return ResponseEntity.badRequest().body(response);
            }

            // Tạo yêu cầu rút tiền sau khi xác thực OTP thành công
            Transaction transaction = transactionService.createWithdrawalRequest(
                user, amount, bankName, accountNumber, accountHolderName, description);

            response.put("success", true);
            response.put("message", "Xác thực OTP thành công! Yêu cầu rút tiền đã được gửi và đang chờ staff duyệt.");
            response.put("transactionId", transaction.getTransactionId());
            response.put("transactionReference", transaction.getTransactionReference());

            log.info("Withdrawal request {} created successfully after OTP verification for user {} with amount {}", 
                transaction.getTransactionId(), user.getUserId(), amount);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid withdrawal request after OTP verification from user {}: {}", userDetails.getUserId(), e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error processing withdrawal request after OTP verification for user {}: {}", 
                userDetails.getUserId(), e.getMessage());
            
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xử lý yêu cầu rút tiền");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xử lý thanh toán nạp tiền qua VietQR
     */
    @PostMapping("/vietqr-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processVietQRPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("amount") Double amount,
            @RequestParam(value = "description", required = false) String description) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate input
            if (amount == null || amount <= 0) {
                response.put("success", false);
                response.put("message", "Số tiền nạp phải lớn hơn 0");
                return ResponseEntity.badRequest().body(response);
            }

            if (amount < 50000) {
                response.put("success", false);
                response.put("message", "Số tiền nạp tối thiểu là 50,000 VNĐ");
                return ResponseEntity.badRequest().body(response);
            }

            if (amount > 50000000) {
                response.put("success", false);
                response.put("message", "Số tiền nạp tối đa là 50,000,000 VNĐ");
                return ResponseEntity.badRequest().body(response);
            }

            // Sử dụng WalletPaymentService để xử lý VietQR
            WalletPaymentService.WalletPaymentResponse vietqrResponse = walletPaymentService.processVietQRWalletDeposit(
                user, amount, description != null ? description : "Nạp tiền qua VietQR");
            
            if (vietqrResponse.getResultCode() == 0) {
                response.put("success", true);
                response.put("message", "Tạo mã QR thanh toán thành công");
                response.put("paymentUrl", vietqrResponse.getPaymentUrl());
                response.put("qrCodeUrl", vietqrResponse.getQrCodeUrl());
                response.put("transactionId", vietqrResponse.getTransactionId());
                
                log.info("Created VietQR wallet deposit order for user {} with amount {}", 
                    user.getUserId(), amount);
            } else {
                response.put("success", false);
                response.put("message", vietqrResponse.getMessage());
                
                log.error("Failed to create VietQR wallet deposit order: {}", vietqrResponse.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing VietQR wallet deposit for user {}: {}", 
                userDetails.getUserId(), e.getMessage());
            
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tạo mã QR thanh toán: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Trang xác nhận thanh toán VietQR
     */
    @GetMapping("/vietqr-confirm")
    public String showVietQRConfirm(@RequestParam("transactionId") String transactionId, 
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                return "redirect:/access-denied";
            }

            // Lấy thông tin giao dịch
            Optional<Transaction> transactionOpt = transactionService.getTransactionById(Integer.parseInt(transactionId));
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                model.addAttribute("transaction", transaction);
                model.addAttribute("amount", transaction.getAmount());
                model.addAttribute("transactionId", transactionId);
                
                return "host/vietqr-confirm";
            } else {
                return "redirect:/host/wallet/nap-tien?error=true&message=" + 
                       URLEncoder.encode("Không tìm thấy thông tin giao dịch", StandardCharsets.UTF_8);
            }
            
        } catch (Exception e) {
            log.error("Error showing VietQR confirm page: {}", e.getMessage());
            return "redirect:/host/wallet/nap-tien?error=true&message=" + 
                   URLEncoder.encode("Có lỗi xảy ra", StandardCharsets.UTF_8);
        }
    }

    /**
     * Xác nhận thanh toán VietQR thành công
     */
    @PostMapping("/vietqr-confirm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmVietQRPayment(
            @RequestParam("transactionId") String transactionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy thông tin giao dịch
            Optional<Transaction> transactionOpt = transactionService.getTransactionById(Integer.parseInt(transactionId));
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                
                // Kiểm tra trạng thái giao dịch
                if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
                    response.put("success", false);
                    response.put("message", "Giao dịch đã được xử lý trước đó");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Cập nhật trạng thái giao dịch - giữ PENDING để chờ staff duyệt
                // Không thêm tiền vào ví ngay lập tức - chờ staff duyệt
                // Chỉ cập nhật thời gian xử lý để đánh dấu thanh toán đã được xác nhận
                transaction.setProcessedAt(LocalDateTime.now());
                transaction.setDescription(transaction.getDescription() + " - Đã thanh toán");
                
                response.put("success", true);
                response.put("message", "Xác nhận thanh toán thành công! Giao dịch đang chờ nhân viên duyệt. Số dư sẽ được cập nhật sau khi được duyệt.");
                
                log.info("VietQR payment confirmed for transaction {} by user {}. Waiting for staff approval.", 
                    transactionId, user.getUserId());
                
                return ResponseEntity.ok(response);
                
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin giao dịch");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error confirming VietQR payment for transaction {}: {}", transactionId, e.getMessage());
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xác nhận thanh toán");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hủy giao dịch nạp tiền
     */
    @PostMapping("/cancel-transaction")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelTransaction(
            @RequestParam("transactionId") String transactionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.OWNER) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy thông tin giao dịch
            Optional<Transaction> transactionOpt = transactionService.getTransactionById(Integer.parseInt(transactionId));
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                
                // Kiểm tra quyền sở hữu giao dịch
                if (!transaction.getUser().getUserId().equals(user.getUserId())) {
                    response.put("success", false);
                    response.put("message", "Không có quyền hủy giao dịch này");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Chỉ có thể hủy giao dịch đang chờ xử lý
                if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
                    response.put("success", false);
                    response.put("message", "Chỉ có thể hủy giao dịch đang chờ xử lý");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Hủy giao dịch bằng cách đánh dấu là FAILED
                transactionService.failTransaction(transaction.getTransactionId(), "Người dùng hủy giao dịch");
                
                response.put("success", true);
                response.put("message", "Giao dịch đã được hủy thành công. Bạn có thể tạo yêu cầu nạp tiền mới.");
                
                log.info("Transaction {} cancelled by user {}", transactionId, user.getUserId());
                
                return ResponseEntity.ok(response);
                
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin giao dịch");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error cancelling transaction {}: {}", transactionId, e.getMessage());
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi hủy giao dịch");
            return ResponseEntity.badRequest().body(response);
        }
    }

}
