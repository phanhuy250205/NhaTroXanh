package nhatroxanh.com.Nhatroxanh.Controller.api;

import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserOwnerRequest;
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

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class UserApiController {
    @Autowired
    private UserService userService;
    // @Autowired
    // private OtpService otpService;
    @Autowired
    private UserRepository userRepository;

   @PostMapping("/register")
public ResponseEntity<?> registerUser(@Valid @RequestBody UserRequest userRequest, BindingResult bindingResult) {
    // Nếu có lỗi validation
    if (bindingResult.hasErrors()) {
        // Duyệt qua các lỗi và trả về message đầu tiên (hoặc gom lại nếu muốn)
        String errorMessage = bindingResult.getFieldErrors().stream()
            .map(error -> error.getDefaultMessage())
            .findFirst() // hoặc .collect(Collectors.joining("\n"))
            .orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(errorMessage);
    }

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
        // if (otpService.verifyOtp(user, otp)) {
        //     return ResponseEntity.ok("Xác thực thành công! Bây giờ bạn có thể đăng nhập.");
        // }
        else {
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
        // otpService.createAndSendOtp(user);
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

        if (!"owner".equalsIgnoreCase(user.getRole().toString())) {
            return ResponseEntity.status(HttpStatus.SC_FORBIDDEN).body("Người dùng không phải là chủ trọ!");
        }

        // Chuẩn bị dữ liệu trả về
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getUserId());
        userData.put("name", user.getFullname() != null ? user.getFullname() : "");
        userData.put("dob", user.getBirthday() != null ? user.getBirthday().toString() : "");
        // Sửa để lấy thông tin CCCD từ CustomUserDetails
        userData.put("cccd", userDetails.getCccd() != null ? userDetails.getCccd() : ""); // Từ Users hoặc UserCccd
        userData.put("cccdNumber", userDetails.getCccdNumber() != null ? userDetails.getCccdNumber() : ""); // Từ UserCccd
        userData.put("issueDate", userDetails.getIssueDate() != null ? userDetails.getIssueDate().toString() : ""); // Từ UserCccd
        userData.put("issuePlace", userDetails.getIssuePlace() != null ? userDetails.getIssuePlace() : ""); // Từ UserCccd
        userData.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("bankAccount", user.getBankAccount() != null ? user.getBankAccount() : "");
        userData.put("address", user.getAddress() != null ? user.getAddress() : "");
        userData.put("role", user.getRole().toString());
        userData.put("username", user.getFullname() != null ? user.getFullname() : "");
        userData.put("isAuthenticated", true);

        System.out.println("Current user data: " + userData);
        return ResponseEntity.ok(userData);
    }

    @PostMapping("/register-owner")
    public ResponseEntity<?> registerOwner(@Valid @RequestBody UserOwnerRequest userOwnerRequest) {
        try {
            userService.registerOwner(userOwnerRequest);
            return ResponseEntity.ok("Đăng ký chủ trọ thành công! Vui lòng kiểm tra email để xác thực tài khoản.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("Đã có lỗi xảy ra trong quá trình đăng ký.");
        }
    }
}