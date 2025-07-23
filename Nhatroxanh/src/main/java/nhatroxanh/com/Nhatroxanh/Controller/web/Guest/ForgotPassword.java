package nhatroxanh.com.Nhatroxanh.Controller.web.Guest;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPass(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Email is missing in forgot-password request");
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp email."));
        }

        Users user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            logger.warn("Email not found: {}", email);
            return ResponseEntity.badRequest().body(Map.of("message", "Email không tồn tại."));
        }

        try {
            otpService.createAndSendOtp(user);
            logger.info("OTP sent successfully to: {}", email);
            return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi đến email của bạn."));
        } catch (Exception e) {
            logger.error("Failed to send OTP to: {}. Error: {}", email, e.getMessage());
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
            logger.warn("Email not found for OTP verification: {}", email);
            return ResponseEntity.badRequest().body(Map.of("message", "Email không tồn tại."));
        }

        boolean isValid = otpService.verifyOtp(user, otp);
        if (isValid) {
            logger.info("OTP verified successfully for: {}", email);
            return ResponseEntity.ok(Map.of("message", "Xác thực OTP thành công."));
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
            logger.warn("Email not found: {}", email);
            return ResponseEntity.badRequest().body(Map.of("message", "Email không tồn tại."));
        }

        try {
            otpService.createAndSendOtp(user);
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
        if (email == null || newPassword == null || email.trim().isEmpty() || newPassword.trim().isEmpty()) {
            logger.warn("Invalid input for reset-password: email={}, newPassword={}", email, newPassword);
            return ResponseEntity.badRequest().body(Map.of("message", "Email và mật khẩu mới không được để trống."));
        }

        Users user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            logger.warn("Email not found: {}", email);
            return ResponseEntity.badRequest().body(Map.of("message", "Email không tồn tại."));
        }

        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setOtpCode(null);
            user.setOtpExpiration(null);
            userRepository.save(user);
            logger.info("Password reset successfully for: {}", email);
            return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công."));
        } catch (Exception e) {
            logger.error("Failed to reset password for: {}. Error: {}", email, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Không thể đặt lại mật khẩu, vui lòng thử lại."));
        }
    }
}