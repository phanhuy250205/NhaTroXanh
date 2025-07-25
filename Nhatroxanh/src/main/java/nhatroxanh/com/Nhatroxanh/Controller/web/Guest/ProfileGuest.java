package nhatroxanh.com.Nhatroxanh.Controller.web.Guest;

import java.io.IOException;
import java.sql.Date;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import nhatroxanh.com.Nhatroxanh.Model.Dto.GuestInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.entity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.FileUploadService;
import nhatroxanh.com.Nhatroxanh.Service.EncryptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/khach-thue")
public class ProfileGuest {

    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EncryptionService encryptionService; // Thêm service mã hóa

    @GetMapping("/profile-khach-thue")
    public String showProfile(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Users user = usersRepository.findById(userDetails.getUser().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserCccd cccd = userCccdRepository.findByUser(user);

        // Tạo DTO để liên kết với form
        GuestInfoDTO dto = new GuestInfoDTO();
        dto.setFullname(user.getFullname());
        dto.setBirthday(user.getBirthday());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setEmail(user.getEmail());
        dto.setAddress(user.getAddress());

        // Giải mã CCCD để hiển thị
        if (cccd != null && cccd.getCccdNumber() != null) {
            try {
                String decryptedCccd = encryptionService.decrypt(cccd.getCccdNumber());
                dto.setCccdNumber(decryptedCccd);
                dto.setIssueDate(cccd.getIssueDate());
                dto.setIssuePlace(cccd.getIssuePlace());
            } catch (Exception e) {
                model.addAttribute("error", "Không thể giải mã CCCD: " + e.getMessage());
                dto.setCccdNumber(null); // Đặt null để tránh hiển thị sai
            }
        }

        model.addAttribute("guestInfo", dto);
        model.addAttribute("user", user);
        return "guest/profile-guest";
    }

    @PostMapping("/profile-khach-thue")
    public String updateProfile(@Valid @ModelAttribute("guestInfo") GuestInfoDTO dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        Users user = usersRepository.findById(userDetails.getUser().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "guest/profile-guest";
        }

        // Kiểm tra trùng email
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            Optional<Users> existingEmail = usersRepository.findByEmail(dto.getEmail().trim());
            if (existingEmail.isPresent() && !existingEmail.get().getUserId().equals(user.getUserId())) {
                model.addAttribute("error", "Email đã được sử dụng bởi tài khoản khác.");
                model.addAttribute("user", user);
                return "guest/profile-guest";
            }
        }

        // Kiểm tra trùng số điện thoại
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            Optional<Users> existingPhone = usersRepository.findByPhone(dto.getPhone().trim());
            if (existingPhone.isPresent() && !existingPhone.get().getUserId().equals(user.getUserId())) {
                model.addAttribute("error", "Số điện thoại đã được sử dụng bởi tài khoản khác.");
                model.addAttribute("user", user);
                return "guest/profile-guest";
            }
        }

        UserCccd cccd = userCccdRepository.findByUser(user);

        // Handle avatar upload
        MultipartFile avatarFile = dto.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                // Kiểm tra loại tệp
                String contentType = avatarFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    model.addAttribute("error", "Chỉ được tải lên tệp ảnh");
                    model.addAttribute("user", user);
                    return "guest/profile-guest";
                }

                // Kiểm tra kích thước tệp (tối đa 5MB)
                if (avatarFile.getSize() > 5 * 1024 * 1024) {
                    model.addAttribute("error", "Kích thước tệp không được vượt quá 5MB");
                    model.addAttribute("user", user);
                    return "guest/profile-guest";
                }

                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    fileUploadService.deleteFile(user.getAvatar());
                }
                String avatarPath = fileUploadService.uploadFile(avatarFile, "");
                user.setAvatar(avatarPath);
            } catch (IOException e) {
                model.addAttribute("error", "Không thể upload ảnh đại diện: " + e.getMessage());
                model.addAttribute("user", user);
                return "guest/profile-guest";
            }
        }

        try {
            // Update user info
            user.setFullname(dto.getFullname());
            user.setBirthday(dto.getBirthday() != null ? new Date(dto.getBirthday().getTime()) : null);
            user.setPhone(dto.getPhone());
            user.setGender(dto.getGender());
            user.setEmail(dto.getEmail());
            user.setAddress(dto.getAddress());

            // Handle CCCD
            if (dto.getCccdNumber() != null && !dto.getCccdNumber().trim().isEmpty()) {
                String trimmedCccd = dto.getCccdNumber().trim();
                // Mã hóa số CCCD
                String encryptedCccd = encryptionService.encrypt(trimmedCccd);
                Optional<UserCccd> existingCccdOptional = userCccdRepository.findByCccdNumber(encryptedCccd);
                if (existingCccdOptional.isPresent()) {
                    UserCccd existingCccd = existingCccdOptional.get();
                    if (cccd == null || !existingCccd.getId().equals(cccd.getId())) {
                        model.addAttribute("error", "Số CCCD đã được sử dụng bởi tài khoản khác.");
                        model.addAttribute("user", user);
                        return "guest/profile-guest";
                    }
                }

                if (cccd == null) {
                    cccd = new UserCccd();
                    cccd.setUser(user);
                }
                cccd.setCccdNumber(encryptedCccd);
                cccd.setIssueDate(dto.getIssueDate() != null ? new Date(dto.getIssueDate().getTime()) : null);
                cccd.setIssuePlace(dto.getIssuePlace() != null && !dto.getIssuePlace().trim().isEmpty()
                        ? dto.getIssuePlace().trim()
                        : null);
                userCccdRepository.save(cccd);
            } else if (cccd != null) {
                userCccdRepository.delete(cccd);
            }

            usersRepository.save(user);
            redirectAttributes.addFlashAttribute("profileSuccess", "Cập nhật thông tin cá nhân thành công!");
            return "redirect:/khach-thue/profile-khach-thue";

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi cập nhật: " + e.getMessage());
            model.addAttribute("user", user);
            return "guest/profile-guest";
        }
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            Users user = usersRepository.findById(userDetails.getUser().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Validation cơ bản
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("passwordError", "Vui lòng nhập mật khẩu hiện tại!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("passwordError", "Vui lòng nhập mật khẩu mới!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("passwordError", "Vui lòng xác nhận mật khẩu mới!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            // Kiểm tra mật khẩu hiện tại
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu hiện tại không đúng!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            // Kiểm tra mật khẩu mới và xác nhận
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu mới và xác nhận mật khẩu không khớp!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            // Kiểm tra độ dài mật khẩu
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu mới phải có ít nhất 6 ký tự!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            // Kiểm tra mật khẩu mới không giống mật khẩu cũ
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu mới phải khác mật khẩu hiện tại!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            // Cập nhật mật khẩu
            user.setPassword(passwordEncoder.encode(newPassword));
            usersRepository.save(user);

            redirectAttributes.addFlashAttribute("passwordSuccess", "Đổi mật khẩu thành công!");
            return "redirect:/khach-thue/profile-khach-thue";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", "Có lỗi xảy ra khi đổi mật khẩu: " + e.getMessage());
            return "redirect:/khach-thue/profile-khach-thue";
        }
    }

    @PostMapping("/report-issue")
    public String reportIssue(@RequestParam("issueTitle") String issueTitle,
            @RequestParam("priority") String priority,
            @RequestParam("description") String description,
            @RequestParam(value = "issueImages", required = false) List<MultipartFile> issueImages,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            Users user = usersRepository.findById(userDetails.getUser().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Validation cơ bản
            if (issueTitle == null || issueTitle.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("reportError", "Vui lòng nhập tiêu đề sự cố!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            if (priority == null || priority.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("reportError", "Vui lòng chọn mức độ ưu tiên!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            if (description == null || description.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("reportError", "Vui lòng mô tả chi tiết sự cố!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            // Validation nâng cao
            if (issueTitle.trim().length() < 5) {
                redirectAttributes.addFlashAttribute("reportError", "Tiêu đề sự cố phải có ít nhất 5 ký tự!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            if (description.trim().length() < 10) {
                redirectAttributes.addFlashAttribute("reportError", "Mô tả sự cố phải có ít nhất 10 ký tự!");
                return "redirect:/khach-thue/profile-khach-thue";
            }

            // Xử lý upload NHIỀU ẢNH
            List<String> imagePaths = new ArrayList<>();
            if (issueImages != null && !issueImages.isEmpty()) {
                // Kiểm tra số lượng ảnh (tối đa 5)
                if (issueImages.size() > 5) {
                    redirectAttributes.addFlashAttribute("reportError", "Chỉ được tải lên tối đa 5 ảnh!");
                    return "redirect:/khach-thue/profile-khach-thue";
                }

                for (MultipartFile image : issueImages) {
                    if (image != null && !image.isEmpty()) {
                        // Kiểm tra loại tệp
                        String contentType = image.getContentType();
                        if (contentType == null || !contentType.startsWith("image/")) {
                            redirectAttributes.addFlashAttribute("reportError",
                                    "File '" + image.getOriginalFilename() + "' không phải là ảnh (JPG, PNG, GIF)!");
                            return "redirect:/khach-thue/profile-khach-thue";
                        }

                        // Kiểm tra kích thước tệp (tối đa 5MB)
                        if (image.getSize() > 5 * 1024 * 1024) {
                            redirectAttributes.addFlashAttribute("reportError",
                                    "File '" + image.getOriginalFilename() + "' vượt quá 5MB!");
                            return "redirect:/khach-thue/profile-khach-thue";
                        }

                        try {
                            String imagePath = fileUploadService.uploadFile(image, "issues/");
                            imagePaths.add(imagePath);
                        } catch (IOException e) {
                            redirectAttributes.addFlashAttribute("reportError",
                                    "Không thể upload ảnh '" + image.getOriginalFilename() + "': " + e.getMessage());
                            return "redirect:/khach-thue/profile-khach-thue";
                        }
                    }
                }
            }

            // TODO: Lưu báo cáo sự cố vào database
            // Ví dụ: issueService.createIssue(user, issueTitle, priority, description,
            // imagePaths);

            // Log thông tin báo cáo (tạm thời để test)
            System.out.println("=== BÁO CÁO SỰ CỐ MỚI ===");
            System.out.println("Người báo cáo: " + user.getFullname() + " (" + user.getEmail() + ")");
            System.out.println("User ID: " + user.getUserId());
            System.out.println("Tiêu đề: " + issueTitle.trim());
            System.out.println("Mức độ ưu tiên: " + priority);
            System.out.println("Mô tả: " + description.trim());
            System.out.println("Số lượng ảnh: " + imagePaths.size());
            if (!imagePaths.isEmpty()) {
                System.out.println("Danh sách ảnh:");
                for (int i = 0; i < imagePaths.size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + imagePaths.get(i));
                }
            }
            System.out.println("Thời gian: " + new java.util.Date());
            System.out.println("========================");

            // Thông báo thành công
            String priorityText = getPriorityText(priority);
            String imageInfo = imagePaths.isEmpty() ? "không có ảnh đính kèm" : imagePaths.size() + " ảnh đính kèm";

            redirectAttributes.addFlashAttribute("reportSuccess",
                    "Báo cáo sự cố \"" + issueTitle.trim() + "\" (Mức độ: " + priorityText + ", " + imageInfo + ") " +
                            "đã được gửi thành công! Chúng tôi sẽ xử lý trong thời gian sớm nhất và liên hệ với bạn qua email "
                            + user.getEmail() + ".");

            return "redirect:/khach-thue/profile-khach-thue";

        } catch (Exception e) {
            System.err.println("Error in reportIssue: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("reportError", "Có lỗi xảy ra khi gửi báo cáo: " + e.getMessage());
            return "redirect:/khach-thue/profile-khach-thue";
        }
    }

    // Helper method để chuyển đổi priority code thành text
    private String getPriorityText(String priority) {
        switch (priority) {
            case "low":
                return "Thấp";
            case "medium":
                return "Trung bình";
            case "high":
                return "Cao";
            case "urgent":
                return "Khẩn cấp";
            default:
                return priority;
        }
    }
}