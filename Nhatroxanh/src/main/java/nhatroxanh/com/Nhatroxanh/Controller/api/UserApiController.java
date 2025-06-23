package nhatroxanh.com.Nhatroxanh.Controller.api;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    @GetMapping("/current")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body("Chưa đăng nhập!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Integer userId = userDetails.getUserId(); // Giả định CustomUserDetails có getUserId()

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại!"));

        if (!"owner".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.SC_FORBIDDEN).body("Người dùng không phải là chủ trọ!");
        }

        // Chuẩn bị dữ liệu trả về
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getUserId());
        userData.put("name", user.getFullname() != null ? user.getFullname() : ""); // Sử dụng fullname thay vì name
        userData.put("dob", user.getBirthday() != null ? user.getBirthday().toString() : ""); // Sử dụng birthday thay
                                                                                              // vì dob
        userData.put("cccd", user.getCccd() != null ? user.getCccd() : "");
        userData.put("issueDate", ""); // Không có issueDate, có thể cần thêm trường nếu cần
        userData.put("issuePlace", ""); // Không có issuePlace, có thể cần thêm trường nếu cần
        userData.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("bankAccount", ""); // Không có bankAccount, có thể cần thêm trường nếu cần
        userData.put("address", user.getAddress() != null ? user.getAddress() : "");
        userData.put("role", user.getRole().toString());
        userData.put("role", user.getRole());

        // Thêm thông tin để kiểm tra tài khoản đăng nhập
        userData.put("username", user.getUsername());
        userData.put("isAuthenticated", true);
        System.out.println("Current user data: " + userData);
        // Trả về thông tin người dùng
        System.out.println("Returning user data: " + userData);
        return ResponseEntity.ok(userData);
    }
}