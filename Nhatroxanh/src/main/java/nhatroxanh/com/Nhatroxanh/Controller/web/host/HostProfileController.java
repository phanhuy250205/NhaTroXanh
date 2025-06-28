package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import java.io.IOException;
import java.sql.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
import nhatroxanh.com.Nhatroxanh.Service.HostelService;

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

    @GetMapping("/profile-host")
    public String showProfile(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Users user = usersRepository.findById(userDetails.getUser().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserCccd cccd = userCccdRepository.findByUser(user);
        int totalHostels = hostelService.countByOwner(user);

        HostInfoDTO dto = new HostInfoDTO();
        dto.setFullname(user.getFullname());
        dto.setBirthday(user.getBirthday());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setEmail(user.getEmail());
        dto.setAddress(user.getAddress());
        dto.setCccd(user.getCccd());
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
            RedirectAttributes redirectAttributes) {

        // Kiểm tra lỗi validation
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Dữ liệu không hợp lệ, vui lòng kiểm tra lại.");
            return "redirect:/chu-tro/profile-host";
        }

        Users user = usersRepository.findById(userDetails.getUser().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserCccd cccd = userCccdRepository.findByUser(user);

        // Handle avatar upload
        MultipartFile avatarFile = dto.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                // Delete old avatar if exists
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    fileUploadService.deleteFile(user.getAvatar());
                }
                // Upload new avatar
                String avatarPath = fileUploadService.uploadFile(avatarFile, "");
                user.setAvatar(avatarPath);
            } catch (IOException e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "Không thể upload ảnh đại diện: " + e.getMessage());
                return "redirect:/chu-tro/profile-host";
            }
        }

        // Update user information
        user.setFullname(dto.getFullname());
        user.setBirthday(dto.getBirthday() != null ? new Date(dto.getBirthday().getTime()) : null);
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setEmail(dto.getEmail());
        user.setCccd(dto.getCccd() != null && !dto.getCccd().trim().isEmpty() ? dto.getCccd().trim() : null);
        user.setAddress(dto.getAddress());

        // Handle CCCD information
        if (dto.getCccd() != null && !dto.getCccd().trim().isEmpty()) {
            // Check for duplicate CCCD number
            UserCccd existingCccd = userCccdRepository.findByCccdNumber(dto.getCccd().trim());
            if (existingCccd != null && (cccd == null || !existingCccd.getId().equals(cccd.getId()))) {
                redirectAttributes.addFlashAttribute("error", "Số CCCD đã được sử dụng bởi tài khoản khác.");
                return "redirect:/chu-tro/profile-host";
            }

            // Update or create CCCD record
            if (cccd == null) {
                cccd = new UserCccd();
                cccd.setUser(user);
            }
            cccd.setCccdNumber(dto.getCccd().trim());
            cccd.setIssueDate(dto.getIssueDate() != null ? new Date(dto.getIssueDate().getTime()) : null);
            cccd.setIssuePlace(
                    dto.getIssuePlace() != null && !dto.getIssuePlace().trim().isEmpty() ? dto.getIssuePlace().trim()
                            : null);
            userCccdRepository.save(cccd);
        } else if (cccd != null) {
            // Remove CCCD record if CCCD is cleared
            userCccdRepository.delete(cccd);
        }

        // Save user after all updates
        usersRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        return "redirect:/chu-tro/profile-host";
    }
}