package nhatroxanh.com.Nhatroxanh.Controller.web.Guest;


import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;

import nhatroxanh.com.Nhatroxanh.Service.OtpCachingService;


import nhatroxanh.com.Nhatroxanh.Service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class ForgotPassword {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPassword.class);

    @Autowired private UserService userService;
    @Autowired private OtpService otpService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

     @Autowired
    private OtpCachingService cachingService;

    @Autowired private NotificationService notificationService;



@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPass(@RequestBody Map<String, String> request) {
    String email = request.get("email");
    if (email == null || email.trim().isEmpty()) {
        logger.warn("Email is missing in forgot-password request");
        return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp email."));
    }

    Users user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
        // Luôn trả về thông báo chung chung để tránh kẻ tấn công dò email
        logger.warn("Attempt to reset password for non-existent email: {}", email);
        return ResponseEntity.ok(Map.of("message", "Nếu email tồn tại, một mã OTP đã được gửi đến bạn."));
    }

    // Kiểm tra xem tài khoản có đang hoạt động không
    if (!user.isEnabled()) {
        logger.warn("Attempt to reset password for a disabled account: {}", email);
        return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản này chưa được kích hoạt hoặc đã bị khóa."));
    }

    try {
        // <<< THAY ĐỔI CHÍNH NẰM Ở ĐÂY >>>
        // Gọi đến phương thức dành riêng cho việc quên mật khẩu
        otpService.createAndSendPasswordResetOtp(user);
        
        logger.info("Password reset OTP sent successfully to: {}", email);
        return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi đến email của bạn."));
    } catch (Exception e) {
        logger.error("Failed to send password reset OTP to: {}. Error: {}", email, e.getMessage());
        return ResponseEntity.status(500).body(Map.of("message", "Không thể gửi OTP, vui lòng thử lại."));
    }
}
@PostMapping("/verify-otp")
public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
    if (email == null || otp == null || email.trim().isEmpty() || otp.trim().isEmpty()) {
        logger.warn("Invalid input for OTP verification: email={}, otp={}", email, otp);
        return ResponseEntity.badRequest().body(Map.of("message", "Email và OTP không được để trống."));
    }

    Users user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
        // Luôn trả về lỗi chung chung để bảo mật
        logger.warn("Attempt to verify OTP for non-existent email: {}", email);
        return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không hợp lệ hoặc đã hết hạn."));
    }

    // <<< THAY ĐỔI CHÍNH BẮT ĐẦU TỪ ĐÂY >>>

    // 1. Gọi đến phương thức xác thực OTP dành riêng cho "Quên mật khẩu"
    boolean isValid = otpService.verifyPasswordResetOtp(user, otp);

    if (isValid) {
        logger.info("OTP verified successfully for: {}", email);
        
        // 2. Tạo ra một token ngẫu nhiên, dùng một lần
        String resetToken = java.util.UUID.randomUUID().toString();
        
        // 3. Lưu token vào cache, key là token và value là email, hết hạn sau 10 phút
        cachingService.cachePasswordResetToken(resetToken, email);
        
        // 4. Trả token này về cho frontend để sử dụng trong bước cuối cùng
        return ResponseEntity.ok(Map.of(
            "message", "Xác thực OTP thành công.",
            "resetToken", resetToken 
        ));
    } else {
        logger.warn("Invalid or expired OTP for: {}", email);
        return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không hợp lệ hoặc đã hết hạn."));
    }
}
@PostMapping("/resend-otp")
public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
    String email = request.get("email");
    if (email == null || email.trim().isEmpty()) {
        logger.warn("Email is missing in resend-otp request");
        return ResponseEntity.badRequest().body(Map.of("message", "Email không được để trống."));
    }

    Users user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
        // Luôn trả về thông báo chung chung để bảo mật
        logger.warn("Attempt to resend OTP for non-existent email: {}", email);
        return ResponseEntity.ok(Map.of("message", "Nếu email của bạn tồn tại trong hệ thống, một mã OTP mới đã được gửi."));
    }

    if (!user.isEnabled()) {
        logger.warn("Attempt to resend OTP for a disabled account: {}", email);
        return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản này chưa được kích hoạt hoặc đã bị khóa."));
    }

    try {
        // <<< THAY ĐỔI CHÍNH: Gọi đến phương thức đúng >>>
        // Tái sử dụng phương thức dành riêng cho việc quên mật khẩu
        otpService.createAndSendPasswordResetOtp(user);
        
        logger.info("Resent OTP successfully to: {}", email);
        return ResponseEntity.ok(Map.of("message", "Mã OTP mới đã được gửi!"));
    } catch (Exception e) {
        logger.error("Failed to resend OTP to: {}. Error: {}", email, e.getMessage());
        return ResponseEntity.status(500).body(Map.of("message", "Không thể gửi OTP, vui lòng thử lại."));
    }
}
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        if (email == null || newPassword == null || confirmPassword == null || 
            email.trim().isEmpty() || newPassword.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            logger.warn("Invalid input for reset-password: email={}, newPassword={}, confirmPassword={}", 
                        email, newPassword, confirmPassword);
            return ResponseEntity.badRequest().body(Map.of("message", "Email, mật khẩu mới và xác nhận mật khẩu không được để trống."));
        }

        if (!newPassword.equals(confirmPassword)) {
            logger.warn("Passwords do not match for: {}", email);
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu mới và xác nhận mật khẩu không khớp."));
        }

        Users user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            logger.warn("Email not found: {}", email);
            return ResponseEntity.badRequest().body(Map.of("message", "Email không tồn tại."));
        }

        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            // Create password recovery notification
            try {
                notificationService.createPasswordRecoveryNotification(user);
                logger.info("Password recovery notification created for user: {}", user.getUserId());
            } catch (Exception notificationException) {
                logger.error("Failed to create password recovery notification for user {}: {}", 
                           user.getUserId(), notificationException.getMessage());
                // Don't fail the password reset if notification creation fails
            }
            
            logger.info("Password reset successfully for: {}", email);
            return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công."));
        } catch (Exception e) {
            logger.error("Failed to reset password for: {}. Error: {}", email, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Không thể đặt lại mật khẩu, vui lòng thử lại."));
        }
    }
}