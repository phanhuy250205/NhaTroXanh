package nhatroxanh.com.Nhatroxanh.Controller.api;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class UserApiController {
    @Autowired
    private UserService userService;
    @Autowired
    private OtpService otpService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRequest userRequest) {
        try {
            userService.registerNewUser(userRequest);
            return ResponseEntity.ok("Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        Users user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("Email không tồn tại.");
        if (user.isEnabled())
            return ResponseEntity.badRequest().body("Tài khoản đã được kích hoạt.");
        if (otpService.verifyOtp(user, otp)) {
            return ResponseEntity.ok("Xác thực thành công! Bây giờ bạn có thể đăng nhập.");
        } else {
            return ResponseEntity.badRequest().body("Mã OTP không hợp lệ hoặc đã hết hạn.");
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {
        Users user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("Email không tồn tại.");
        if (user.isEnabled())
            return ResponseEntity.badRequest().body("Tài khoản này đã được kích hoạt.");
        otpService.createAndSendOtp(user);
        return ResponseEntity.ok("Đã gửi lại mã OTP. Vui lòng kiểm tra email.");
    }
}