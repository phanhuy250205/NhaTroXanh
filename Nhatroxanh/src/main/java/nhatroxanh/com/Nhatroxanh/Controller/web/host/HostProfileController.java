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

    // 1. Kiểm tra lỗi validation cơ bản
    if (bindingResult.hasErrors()) {
        redirectAttributes.addFlashAttribute("error", "Dữ liệu không hợp lệ, vui lòng kiểm tra lại.");
        return "redirect:/chu-tro/profile-host";
    }

    try {
        Users user = usersRepository.findById(userDetails.getUser().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userDetails.getUser().getUserId()));
        MultipartFile avatarFile = dto.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                fileUploadService.deleteFile(user.getAvatar());
            }
            String avatarPath = fileUploadService.uploadFile(avatarFile, "");
            user.setAvatar(avatarPath);
        }
        user.setFullname(dto.getFullname());
        user.setBirthday(dto.getBirthday() != null ? new Date(dto.getBirthday().getTime()) : null);
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setEmail(dto.getEmail());
        user.setAddress(dto.getAddress());
        UserCccd cccd = userCccdRepository.findByUser(user);
        String newCccdNumber = (dto.getCccd() != null && !dto.getCccd().trim().isEmpty()) ? dto.getCccd().trim() : null;
        if (newCccdNumber != null) {
            UserCccd existingCccd = userCccdRepository.findByCccdNumber(newCccdNumber);
            if (existingCccd != null && !existingCccd.getUser().getUserId().equals(user.getUserId())) {
                redirectAttributes.addFlashAttribute("error", "Số CCCD đã được sử dụng bởi một tài khoản khác.");
                return "redirect:/chu-tro/profile-host";
            }
            if (cccd == null) {
                cccd = new UserCccd();
                cccd.setUser(user);
            }
            cccd.setCccdNumber(newCccdNumber);
            cccd.setIssueDate(dto.getIssueDate() != null ? new Date(dto.getIssueDate().getTime()) : null);
            cccd.setIssuePlace(dto.getIssuePlace());     
            userCccdRepository.save(cccd);
        } else if (cccd != null) {
            userCccdRepository.delete(cccd);
        }
        usersRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");

    } catch (Exception e) {
        e.printStackTrace();
        redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi khi cập nhật: " + e.getMessage());
    }
    return "redirect:/chu-tro/profile-host";
}
}