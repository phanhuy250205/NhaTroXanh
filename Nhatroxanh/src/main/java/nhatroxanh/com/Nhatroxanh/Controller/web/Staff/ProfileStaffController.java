package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import java.io.IOException;
import java.sql.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

import jakarta.transaction.Transactional;
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
@RequestMapping("/nhan-vien")
public class ProfileStaffController {

    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private HostelService hostelService;

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/profile-nhan-vien")
    public String showProfile(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Users user = usersRepository.findById(userDetails.getUser().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            UserCccd cccd = userCccdRepository.findByUser(user);
            int totalHostels = hostelService.countByOwner(user);

            // Tạo DTO để binding với form
            HostInfoDTO dto = new HostInfoDTO();
            dto.setFullname(user.getFullname());
            dto.setBirthday(user.getBirthday());
            dto.setPhone(user.getPhone());
            dto.setGender(user.getGender());
            dto.setEmail(user.getEmail());
            dto.setAddress(user.getAddress());
            if (cccd != null) {
                dto.setCccdNumber(cccd.getCccdNumber());
                dto.setIssueDate(cccd.getIssueDate());
                dto.setIssuePlace(cccd.getIssuePlace());
            }

            model.addAttribute("hostInfo", dto);
            model.addAttribute("user", user);
            model.addAttribute("totalHostels", totalHostels);
            model.addAttribute("cccd", cccd);

            return "staff/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải thông tin: " + e.getMessage());
            return "error/500";
        }
    }

    @PostMapping("/profile-nhan-vien")
    @Transactional
    public String updateProfile(@Valid @ModelAttribute("hostInfo") HostInfoDTO dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Users user = usersRepository.findById(userDetails.getUser().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Kiểm tra validation errors
            if (bindingResult.hasErrors()) {
                model.addAttribute("user", user);
                model.addAttribute("totalHostels", hostelService.countByOwner(user));
                model.addAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin đã nhập");
                return "staff/profile";
            }

            // Kiểm tra trùng email
            if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
                Optional<Users> existingEmailUser = usersRepository.findByEmail(dto.getEmail().trim());
                if (existingEmailUser.isPresent() && !existingEmailUser.get().getUserId().equals(user.getUserId())) {
                    model.addAttribute("errorMessage", "Email đã được sử dụng bởi tài khoản khác.");
                    model.addAttribute("user", user);
                    model.addAttribute("totalHostels", hostelService.countByOwner(user));
                    return "staff/profile";
                }
            }

            // Kiểm tra trùng số điện thoại
            if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
                Optional<Users> existingPhoneUser = usersRepository.findByPhone(dto.getPhone().trim());
                if (existingPhoneUser.isPresent() && !existingPhoneUser.get().getUserId().equals(user.getUserId())) {
                    model.addAttribute("errorMessage", "Số điện thoại đã được sử dụng bởi tài khoản khác.");
                    model.addAttribute("user", user);
                    model.addAttribute("totalHostels", hostelService.countByOwner(user));
                    return "staff/profile";
                }
            }

            // Xử lý upload avatar
            MultipartFile avatarFile = dto.getAvatarFile();
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    // Validate file type
                    String contentType = avatarFile.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        model.addAttribute("errorMessage", "Chỉ được upload file ảnh");
                        model.addAttribute("user", user);
                        model.addAttribute("totalHostels", hostelService.countByOwner(user));
                        return "staff/profile";
                    }

                    // Validate file size (max 5MB)
                    if (avatarFile.getSize() > 5 * 1024 * 1024) {
                        model.addAttribute("errorMessage", "Kích thước file không được vượt quá 5MB");
                        model.addAttribute("user", user);
                        model.addAttribute("totalHostels", hostelService.countByOwner(user));
                        return "staff/profile";
                    }

                    // Delete old avatar if exists
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        fileUploadService.deleteFile(user.getAvatar());
                    }

                    // Upload new avatar
                    String avatarPath = fileUploadService.uploadFile(avatarFile, "");
                    user.setAvatar(avatarPath);
                } catch (IOException e) {
                    model.addAttribute("errorMessage", "Không thể upload ảnh đại diện: " + e.getMessage());
                    model.addAttribute("user", user);
                    model.addAttribute("totalHostels", hostelService.countByOwner(user));
                    return "staff/profile";
                }
            }

            // Cập nhật thông tin user
            user.setFullname(dto.getFullname());
            user.setBirthday(dto.getBirthday() != null ? new Date(dto.getBirthday().getTime()) : null);
            user.setPhone(dto.getPhone());
            user.setGender(dto.getGender());
            user.setEmail(dto.getEmail());
            user.setAddress(dto.getAddress());

            // Xử lý thông tin CCCD
            UserCccd cccd = userCccdRepository.findByUser(user);

            if (dto.getCccdNumber() != null && !dto.getCccdNumber().trim().isEmpty()) {
                String trimmedCccd = dto.getCccdNumber().trim();

                // Kiểm tra CCCD đã tồn tại chưa
                Optional<UserCccd> existingCccdOptional = userCccdRepository.findByCccdNumber(trimmedCccd);
                if (existingCccdOptional.isPresent()) {
                    UserCccd existingCccd = existingCccdOptional.get();
                    if (cccd == null || !existingCccd.getId().equals(cccd.getId())) {
                        model.addAttribute("errorMessage", "Số CCCD đã được sử dụng bởi tài khoản khác.");
                        model.addAttribute("user", user);
                        model.addAttribute("totalHostels", hostelService.countByOwner(user));
                        return "staff/profile";
                    }
                }

                // Tạo mới hoặc cập nhật CCCD
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
                // Xóa CCCD nếu không nhập số CCCD
                userCccdRepository.delete(cccd);
            }

            // Lưu thông tin user chỉ khi không có lỗi
            usersRepository.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
            return "redirect:/nhan-vien/profile-nhan-vien";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/nhan-vien/profile-nhan-vien";
        }
    }
}
