package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import java.io.IOException;
import java.sql.Date;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import nhatroxanh.com.Nhatroxanh.Model.entity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.FileUploadService;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.TenantService;
import nhatroxanh.com.Nhatroxanh.Service.NotificationService;

@Controller
@RequestMapping("/chu-tro")
public class HostProfileController {

    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private HostelService hostelService;

    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private TenantService tenantService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/profile-host")
    public String showProfile(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Users user = usersRepository.findById(userDetails.getUser().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserCccd cccd = userCccdRepository.findByUser(user); // không dùng .orElse(null)

        int totalHostels = hostelService.countByOwner(user);

        HostInfoDTO dto = new HostInfoDTO();
        dto.setFullname(user.getFullname());
        dto.setBirthday(user.getBirthday());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setEmail(user.getEmail());
        dto.setAddress(user.getAddress());
        dto.setCccdNumber(cccd != null ? cccd.getCccdNumber() : null);

        if (cccd != null) {
            dto.setIssueDate(cccd.getIssueDate());
            dto.setIssuePlace(cccd.getIssuePlace());
        }

        model.addAttribute("hostInfo", dto);
        model.addAttribute("user", user);
        model.addAttribute("totalHostels", totalHostels);
        return "host/profile-host";
    }

    @PostMapping("/profile-host")
    public String updateProfile(@Valid @ModelAttribute("hostInfo") HostInfoDTO dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        Users user = usersRepository.findById(userDetails.getUser().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("hostInfo", dto);
            model.addAttribute("user", user);
            model.addAttribute("totalHostels", hostelService.countByOwner(user));
            return "host/profile-host";
        }

        // ✅ Kiểm tra trùng email
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            Optional<Users> existingEmail = usersRepository.findByEmail(dto.getEmail().trim());
            if (existingEmail.isPresent() && !existingEmail.get().getUserId().equals(user.getUserId())) {
                model.addAttribute("hostInfo", dto);
                model.addAttribute("user", user);
                model.addAttribute("totalHostels", hostelService.countByOwner(user));
                model.addAttribute("error", "Email đã được sử dụng bởi tài khoản khác.");
                return "host/profile-host";
            }
        }

        // ✅ Kiểm tra trùng số điện thoại
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            Optional<Users> existingPhone = usersRepository.findByPhone(dto.getPhone().trim());
            if (existingPhone.isPresent() && !existingPhone.get().getUserId().equals(user.getUserId())) {
                model.addAttribute("hostInfo", dto);
                model.addAttribute("user", user);
                model.addAttribute("totalHostels", hostelService.countByOwner(user));
                model.addAttribute("error", "Số điện thoại đã được sử dụng bởi tài khoản khác.");
                return "host/profile-host";
            }
        }

        UserCccd cccd = userCccdRepository.findByUser(user);

        MultipartFile avatarFile = dto.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    fileUploadService.deleteFile(user.getAvatar());
                }
                String avatarPath = fileUploadService.uploadFile(avatarFile, "");
                user.setAvatar(avatarPath);
            } catch (IOException e) {
                model.addAttribute("hostInfo", dto);
                model.addAttribute("user", user);
                model.addAttribute("totalHostels", hostelService.countByOwner(user));
                model.addAttribute("error", "Không thể upload ảnh đại diện: " + e.getMessage());
                return "host/profile-host";
            }
        }

        try {
            // Store original values for comparison
            Users originalUser = new Users();
            originalUser.setFullname(user.getFullname());
            originalUser.setEmail(user.getEmail());
            originalUser.setPhone(user.getPhone());
            originalUser.setAddress(user.getAddress());
            originalUser.setGender(user.getGender());
            originalUser.setBirthday(user.getBirthday());

            // Cập nhật user
            user.setFullname(dto.getFullname());
            user.setBirthday(dto.getBirthday() != null ? new Date(dto.getBirthday().getTime()) : null);
            user.setPhone(dto.getPhone());
            user.setGender(dto.getGender());
            user.setEmail(dto.getEmail());
            user.setAddress(dto.getAddress());

            // Xử lý CCCD
            if (dto.getCccdNumber() != null && !dto.getCccdNumber().trim().isEmpty()) {
                String trimmedCccd = dto.getCccdNumber().trim();
                Optional<UserCccd> existingCccdOptional = userCccdRepository.findByCccdNumber(trimmedCccd);
                if (existingCccdOptional.isPresent()) {
                    UserCccd existingCccd = existingCccdOptional.get();
                    if (cccd == null || !existingCccd.getId().equals(cccd.getId())) {
                        model.addAttribute("hostInfo", dto);
                        model.addAttribute("user", user);
                        model.addAttribute("totalHostels", hostelService.countByOwner(user));
                        model.addAttribute("error", "Số CCCD đã được sử dụng bởi tài khoản khác.");
                        return "host/profile-host";
                    }
                }

                if (cccd == null) {
                    cccd = new UserCccd();
                    cccd.setUser(user);
                }
                cccd.setCccdNumber(trimmedCccd);
                cccd.setIssueDate(dto.getIssueDate() != null ? new Date(dto.getIssueDate().getTime()) : null);
                cccd.setIssuePlace(dto.getIssuePlace() != null && !dto.getIssuePlace().trim().isEmpty()
                        ? dto.getIssuePlace().trim()
                        : null);

                userCccdRepository.save(cccd);
            } else if (cccd != null) {
                userCccdRepository.delete(cccd);
            }

            usersRepository.save(user);

            // Create profile update notification
            try {
                java.util.List<String> updatedFields = new java.util.ArrayList<>();
                if (!java.util.Objects.equals(originalUser.getFullname(), user.getFullname())) {
                    updatedFields.add("Họ tên");
                }
                if (!java.util.Objects.equals(originalUser.getEmail(), user.getEmail())) {
                    updatedFields.add("Email");
                }
                if (!java.util.Objects.equals(originalUser.getPhone(), user.getPhone())) {
                    updatedFields.add("Số điện thoại");
                }
                if (!java.util.Objects.equals(originalUser.getAddress(), user.getAddress())) {
                    updatedFields.add("Địa chỉ");
                }
                if (!java.util.Objects.equals(originalUser.getGender(), user.getGender())) {
                    updatedFields.add("Giới tính");
                }
                if (!java.util.Objects.equals(originalUser.getBirthday(), user.getBirthday())) {
                    updatedFields.add("Ngày sinh");
                }
                if (avatarFile != null && !avatarFile.isEmpty()) {
                    updatedFields.add("Ảnh đại diện");
                }

                if (!updatedFields.isEmpty()) {
                    String fieldsString = String.join(", ", updatedFields);
                    notificationService.createProfileUpdateNotification(user, fieldsString);
                }
            } catch (Exception e) {
                // Log error but don't fail the update
                System.err.println("Failed to create profile update notification: " + e.getMessage());
            }

            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi khi cập nhật: " + e.getMessage());
        }

        return "redirect:/chu-tro/profile-host";
    }

    @PostMapping("/chi-tiet-khach-thue/update")

    public String updateTenantStatus(@RequestParam("contractId") Integer contractId,
            @RequestParam("status") Boolean newStatus,
            RedirectAttributes redirectAttributes) {


        try {
            tenantService.updateContractStatus(contractId, newStatus);
            // Gửi một thông báo thành công về trang chi tiết
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            // Gửi một thông báo lỗi về trang chi tiết
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/chu-tro/chi-tiet-khach-thue/" + contractId;
    }
}