package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.EncryptionService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

@Controller
@RequestMapping("/nhan-vien")
public class OwnerController {

    private static final Logger logger = LoggerFactory.getLogger(OwnerController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EncryptionService encryptionService;

    @GetMapping("/chu-tro")
    public String listOwners(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(defaultValue = "0") int approvalPage,
            @RequestParam(required = false) String approvalSearch,
            @RequestParam(defaultValue = "manage") String activeTab) {

        // Quản Lý Chủ Trọ tab
        Page<Users> owners = userService.getFilteredOwners(search, statusFilter, page, size);
        model.addAttribute("owners", owners);
        model.addAttribute("currentPage", page);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);

        // Duyệt Đăng Ký tab
        Page<Users> pendingOwners = userService.getPendingOwners(approvalPage, size, approvalSearch);
        model.addAttribute("pendingOwners", pendingOwners);
        model.addAttribute("approvalPage", approvalPage);
        model.addAttribute("approvalSearch", approvalSearch);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("modalFragment", "chu-tro");

        return "staff/chu-tro";
    }

    @GetMapping("/chi-tiet-chu-tro/{id}")
    public String showOwnerDetail(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Users> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
            return "redirect:/nhan-vien/chu-tro";
        }

        Users user = optionalUser.get();
        UserCccd userCccd = userCccdRepository.findByUser(user);
        if (userCccd != null && userCccd.getCccdNumber() != null) {
            try {
                String decryptedCccd = encryptionService.decrypt(userCccd.getCccdNumber());
                userCccd.setCccdNumber(decryptedCccd); // Tạm thời gán giá trị giải mã để hiển thị
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Không thể giải mã CCCD: " + e.getMessage());
            }
        }
        List<Hostel> hostels = hostelRepository.findByOwner(user);

        model.addAttribute("user", user);
        model.addAttribute("cccd", userCccd);
        model.addAttribute("hostels", hostels);

        return "staff/chitiet-chutro";
    }

    @PostMapping("/chu-tro/toggle-enable")
    public String toggleEnable(@RequestParam("userId") Integer userId, RedirectAttributes redirectAttributes) {
        Optional<Users> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
            String status = user.isEnabled() ? "mở khóa" : "vô hiệu hóa";
            redirectAttributes.addFlashAttribute("successMessage", "Chủ trọ đã được " + status + " thành công.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
        }
        return "redirect:/nhan-vien/chi-tiet-chu-tro/" + userId;
    }

    @PostMapping("/chu-tro/approve-owner/{id}")
    public String approveOwner(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            userService.approveOwner(id);
            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt chủ trọ thành công.");
        } catch (RuntimeException e) {
            logger.error("Error approving owner with ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error approving owner with ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi xảy ra khi phê duyệt.");
        }
        return "redirect:/nhan-vien/chu-tro?activeTab=approve";
    }

    @PostMapping("/chu-tro/reject-owner/{id}")
    public String rejectOwner(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            userService.rejectOwner(id);
            redirectAttributes.addFlashAttribute("successMessage", "Từ chối đăng ký chủ trọ thành công.");
        } catch (RuntimeException e) {
            logger.error("Error rejecting owner with ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error rejecting owner with ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi xảy ra khi từ chối.");
        }
        return "redirect:/nhan-vien/chu-tro?activeTab=approve";
    }

    @GetMapping("/chu-tro/chi-tiet-duyet/{id}")
    public String showPendingOwnerDetail(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes,
            @RequestParam(defaultValue = "0") int approvalPage,
            @RequestParam(required = false) String approvalSearch) {
        Optional<Users> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
            return "redirect:/nhan-vien/chu-tro?activeTab=approve&approvalPage=" + approvalPage + "&approvalSearch="
                    + (approvalSearch != null ? approvalSearch : "");
        }

        Users user = optionalUser.get();
        if (!Users.Role.OWNER.equals(user.getRole()) || !Users.Status.PENDING.equals(user.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng không hợp lệ để xem chi tiết duyệt.");
            return "redirect:/nhan-vien/chu-tro?activeTab=approve&approvalPage=" + approvalPage + "&approvalSearch="
                    + (approvalSearch != null ? approvalSearch : "");
        }

        UserCccd userCccd = userCccdRepository.findByUser(user);
        if (userCccd != null && userCccd.getCccdNumber() != null) {
            try {
                String decryptedCccd = encryptionService.decrypt(userCccd.getCccdNumber());
                userCccd.setCccdNumber(decryptedCccd); // Tạm thời gán giá trị giải mã để hiển thị
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Không thể giải mã CCCD: " + e.getMessage());
            }
        }
        model.addAttribute("user", user);
        model.addAttribute("cccd", userCccd);
        model.addAttribute("approvalPage", approvalPage);
        model.addAttribute("approvalSearch", approvalSearch);

        return "staff/chi-tiet-dang-ki";
    }
}