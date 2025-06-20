package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        Users user = usersRepository.findById(userDetails.getUser().getUserId()).orElseThrow();
        UserCccd cccd = userCccdRepository.findByUser(user);
        int totalHostels = hostelService.countByOwner(user);
        Users users = userDetails.getUser();

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
    public String updateProfile(@ModelAttribute("hostInfo") HostInfoDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        Users user = usersRepository.findById(userDetails.getUser().getUserId()).orElseThrow();
        UserCccd cccd = userCccdRepository.findByUser(user);

        // Cập nhật ảnh đại diện nếu có file mới
        MultipartFile avatarFile = dto.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                // Xóa ảnh cũ nếu có
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    fileUploadService.deleteFile(user.getAvatar());
                }
                // Upload file mới
                String avatarPath = fileUploadService.uploadFile(avatarFile, "");
                user.setAvatar(avatarPath);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Không thể upload ảnh đại diện.");
                return "redirect:/chu-tro/profile-host";
            }
        }

        // Cập nhật thông tin user
        user.setFullname(dto.getFullname());
        user.setBirthday(new java.sql.Date(dto.getBirthday().getTime()));
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setEmail(dto.getEmail());
        user.setCccd(dto.getCccd());
        user.setAddress(dto.getAddress()); // nếu address là String
        usersRepository.save(user);

        // Cập nhật CCCD
        if (cccd == null) {
            cccd = new UserCccd();
            cccd.setUser(user);
        }
        cccd.setCccdNumber(dto.getCccd());
        cccd.setIssueDate(new java.sql.Date(dto.getIssueDate().getTime()));
        cccd.setIssuePlace(dto.getIssuePlace());
        userCccdRepository.save(cccd);

        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        return "redirect:/chu-tro/profile-host";
    }
}
