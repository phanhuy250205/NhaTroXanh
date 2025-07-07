package nhatroxanh.com.Nhatroxanh.Controller.web.admin;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import nhatroxanh.com.Nhatroxanh.Model.Dto.HostInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.FileUploadService;

@Controller
@RequestMapping("/admin/profile")
public class ProfileAdminController {
    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping
    public String showProfile(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        Users user = usersRepository.findById(currentUser.getUserId()).orElse(null);
        model.addAttribute("user", user);
        return "admin/profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            @RequestParam("fullname") String fullname,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam(value = "birthday", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate birthday,
            @RequestParam(value = "gender", required = false) Boolean gender,
            @RequestParam(value = "cccdNumber", required = false) String cccdNumber,
            @RequestParam(value = "issueDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate issueDate,
            @RequestParam(value = "issuePlace", required = false) String issuePlace,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Users user = usersRepository.findById(currentUser.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Validation
            if (fullname == null || fullname.trim().isEmpty()) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "Họ tên không được để trống");
                return "admin/profile";
            }
            if (fullname.length() < 2 || fullname.length() > 100) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "Họ tên phải từ 2 đến 100 ký tự");
                return "admin/profile";
            }
            if (email == null || email.trim().isEmpty()) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "Email không được để trống");
                return "admin/profile";
            }
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "Email không đúng định dạng");
                return "admin/profile";
            }
            if (phone == null || phone.trim().isEmpty()) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "Số điện thoại không được để trống");
                return "admin/profile";
            }
            if (!phone.matches("^0[0-9]{9}$")) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "Số điện thoại phải có 10 chữ số và bắt đầu bằng số 0");
                return "admin/profile";
            }
            if (cccdNumber != null && !cccdNumber.trim().isEmpty() && !cccdNumber.matches("^[0-9]{12}$")) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "CCCD phải có đúng 12 chữ số");
                return "admin/profile";
            }
            if (birthday != null) {
                LocalDate today = LocalDate.now();
                int age = today.getYear() - birthday.getYear();
                if (age < 18 || age > 65) {
                    model.addAttribute("user", user);
                    model.addAttribute("errorMessage", "Tuổi phải từ 18 đến 65");
                    return "admin/profile";
                }
            }
            if (issueDate != null && birthday != null && issueDate.isBefore(birthday)) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "Ngày cấp CCCD phải sau ngày sinh");
                return "admin/profile";
            }
            if (issueDate != null && issueDate.isAfter(LocalDate.now())) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "Ngày cấp CCCD không được là tương lai");
                return "admin/profile";
            }
            if (issuePlace != null && !issuePlace.trim().isEmpty() && issuePlace.trim().length() < 2) {
                model.addAttribute("user", user);
                model.addAttribute("errorMessage", "Nơi cấp CCCD phải có ít nhất 2 ký tự");
                return "admin/profile";
            }

            // Xử lý upload avatar
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    String contentType = avatarFile.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        model.addAttribute("errorMessage", "Chỉ được upload file ảnh");
                        model.addAttribute("user", user);
                        return "admin/profile";
                    }

                    if (avatarFile.getSize() > 5 * 1024 * 1024) {
                        model.addAttribute("errorMessage", "Kích thước file không được vượt quá 5MB");
                        model.addAttribute("user", user);
                        return "admin/profile";
                    }

                    // Xóa ảnh cũ nếu có
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        fileUploadService.deleteFile(user.getAvatar());
                    }

                    // Upload và lưu đường dẫn
                    String avatarPath = fileUploadService.uploadFile(avatarFile, "");
                    user.setAvatar(avatarPath);

                } catch (IOException e) {
                    model.addAttribute("errorMessage", "Không thể upload ảnh đại diện: " + e.getMessage());
                    model.addAttribute("user", user);
                    return "admin/profile";
                }
            }

            // Cập nhật thông tin user
            user.setFullname(fullname.trim());
            user.setEmail(email.trim());
            user.setPhone(phone.trim());
            user.setGender(gender);
            user.setBirthday(birthday != null ? Date.valueOf(birthday) : null);
            user.setAddress(address != null ? address.trim() : null);

            // Xử lý CCCD
            UserCccd cccd = userCccdRepository.findByUser(user);
            if (cccdNumber != null && !cccdNumber.trim().isEmpty()) {
                String trimmedCccd = cccdNumber.trim();
                Optional<UserCccd> existingCccdOptional = userCccdRepository.findByCccdNumber(trimmedCccd);
                if (existingCccdOptional.isPresent()) {
                    UserCccd existingCccd = existingCccdOptional.get();
                    if (cccd == null || !existingCccd.getId().equals(cccd.getId())) {
                        model.addAttribute("user", user);
                        model.addAttribute("errorMessage", "Số CCCD đã được sử dụng bởi tài khoản khác.");
                        return "admin/profile";
                    }
                }

                if (cccd == null) {
                    cccd = new UserCccd();
                    cccd.setUser(user);
                }

                cccd.setCccdNumber(trimmedCccd);
                cccd.setIssueDate(issueDate != null ? Date.valueOf(issueDate) : null);
                cccd.setIssuePlace(issuePlace != null && !issuePlace.trim().isEmpty() ? issuePlace.trim() : null);
                userCccdRepository.save(cccd);

            } else if (cccd != null) {
                userCccdRepository.delete(cccd);
            }

            usersRepository.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
            return "redirect:/admin/profile";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/profile";
        }
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {

        Users user = currentUser.getUser();

        // Kiểm tra xác nhận mật khẩu
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp.");
            return "redirect:/admin/profile#security";
        }

        // So sánh mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu hiện tại không đúng.");
            return "redirect:/admin/profile#security";
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        return "redirect:/admin/profile#security";
    }

}
