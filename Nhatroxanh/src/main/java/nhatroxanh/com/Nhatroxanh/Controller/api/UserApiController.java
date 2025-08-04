package nhatroxanh.com.Nhatroxanh.Controller.api;

import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserOwnerRequest;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.io.IOException;
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
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRequest userRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .findFirst()
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
        return ResponseEntity.ok("Đã gửi lại mã OTP. Vui lòng kiểm tra email.");
    }

    // API cho chat system - lấy thông tin user hiện tại
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // Kiểm tra authentication theo Spring Security best practices [[2]](#__2)
            if (auth == null || !auth.isAuthenticated() ||
                    "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Chưa đăng nhập!");
            }

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            Integer userId = userDetails.getUserId();

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại!"));

            // Chuẩn bị dữ liệu cho chat system
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getUserId()); // Đổi từ "id" thành "userId" cho chat
            userData.put("fullname", user.getFullname() != null ? user.getFullname() : "");
            userData.put("email", user.getEmail() != null ? user.getEmail() : "");
            userData.put("phone", user.getPhone() != null ? user.getPhone() : "");
            userData.put("avatar", user.getAvatar() != null ? user.getAvatar() : ""); // Thêm avatar cho chat
            userData.put("role", user.getRole().toString());
            userData.put("isAuthenticated", true);

            // Thông tin bổ sung cho owner
            if ("OWNER".equalsIgnoreCase(user.getRole().toString())) {
                userData.put("dob", user.getBirthday() != null ? user.getBirthday().toString() : "");
                userData.put("cccd", userDetails.getCccd() != null ? userDetails.getCccd() : "");
                userData.put("cccdNumber", userDetails.getCccdNumber() != null ? userDetails.getCccdNumber() : "");
                userData.put("issueDate", userDetails.getIssueDate() != null ? userDetails.getIssueDate().toString() : "");
                userData.put("issuePlace", userDetails.getIssuePlace() != null ? userDetails.getIssuePlace() : "");
                userData.put("bankAccount", user.getBankAccount() != null ? user.getBankAccount() : "");
                userData.put("address", user.getAddress() != null ? user.getAddress() : "");
            }

            System.out.println("Current user data: " + userData);
            return ResponseEntity.ok(userData);

        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin người dùng");
        }
    }

    // API riêng cho owner (giữ nguyên logic cũ)
    @GetMapping("/current-owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> getCurrentOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Integer userId = userDetails.getUserId();

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại!"));

        if (!"OWNER".equalsIgnoreCase(user.getRole().toString())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Người dùng không phải là chủ trọ!");
        }

        // Dữ liệu chi tiết cho owner
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getUserId());
        userData.put("name", user.getFullname() != null ? user.getFullname() : "");
        userData.put("dob", user.getBirthday() != null ? user.getBirthday().toString() : "");
        userData.put("cccd", userDetails.getCccd() != null ? userDetails.getCccd() : "");
        userData.put("cccdNumber", userDetails.getCccdNumber() != null ? userDetails.getCccdNumber() : "");
        userData.put("issueDate", userDetails.getIssueDate() != null ? userDetails.getIssueDate().toString() : "");
        userData.put("issuePlace", userDetails.getIssuePlace() != null ? userDetails.getIssuePlace() : "");
        userData.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("bankAccount", user.getBankAccount() != null ? user.getBankAccount() : "");
        userData.put("address", user.getAddress() != null ? user.getAddress() : "");
        userData.put("role", user.getRole().toString());
        userData.put("username", user.getFullname() != null ? user.getFullname() : "");
        userData.put("isAuthenticated", true);

        return ResponseEntity.ok(userData);
    }

    @PostMapping(value = "/register-owner", consumes = "multipart/form-data")
    public ResponseEntity<?> registerOwner(
            @Valid @ModelAttribute UserOwnerRequest userOwnerRequest,
            @RequestPart(value = "frontImage", required = true) MultipartFile frontImage,
            @RequestPart(value = "backImage", required = true) MultipartFile backImage) {
        try {
            userService.registerOwner(userOwnerRequest, frontImage, backImage);
            return ResponseEntity.ok("Đăng ký chủ trọ thành công! Vui lòng kiểm tra email để xác thực tài khoản.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã có lỗi xảy ra trong quá trình đăng ký.");
        }
    }

    // API kiểm tra trạng thái đăng nhập cho chat
    @GetMapping("/check-auth")
    public ResponseEntity<?> checkAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();

        if (auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal())) {
            response.put("isAuthenticated", true);
            response.put("role", auth.getAuthorities().toString());
        } else {
            response.put("isAuthenticated", false);
            response.put("message", "Chưa đăng nhập");
        }

        return ResponseEntity.ok(response);
    }
}
