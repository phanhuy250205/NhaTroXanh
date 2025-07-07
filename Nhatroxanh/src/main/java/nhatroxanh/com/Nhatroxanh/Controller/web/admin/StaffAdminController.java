package nhatroxanh.com.Nhatroxanh.Controller.web.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

@Controller
@RequestMapping("/admin/quan-ly-nhan-vien")
public class StaffAdminController {
    @Autowired
    private UserService usersService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @GetMapping
    public String listStaffUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            Model model) {

        long totalStaff = userRepository.countByRole(Users.Role.STAFF);
        long activeStaff = userRepository.countByRoleAndEnabled(Users.Role.STAFF, true);
        long inactiveStaff = userRepository.countByRoleAndEnabled(Users.Role.STAFF, false);
        Page<Users> staffPage = usersService.searchAndFilterStaffUsers(page, size, keyword, status);
        model.addAttribute("totalStaff", totalStaff);
        model.addAttribute("activeStaff", activeStaff);
        model.addAttribute("inactiveStaff", inactiveStaff);

        model.addAttribute("staffPage", staffPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", staffPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", status);

        return "admin/quan-ly-nhan-vien";
    }

    @GetMapping("/{id}")
    public String viewStaffDetail(@PathVariable("id") Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {
        Page<Users> staffPage = usersService.getStaffUsers(page, size);
        Users staff = usersService.getById(id);

        model.addAttribute("staffDetail", staff);
        model.addAttribute("staffPage", staffPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", staffPage.getTotalPages());

        return "admin/quan-ly-nhan-vien";
    }

    @PostMapping("/toggle-enable")
    public String toggleEnable(@RequestParam("userId") Integer userId, RedirectAttributes redirectAttributes) {
        Optional<Users> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
            String status = user.isEnabled() ? "Kích hoạt" : "vô hiệu hóa";
            redirectAttributes.addFlashAttribute("successMessage", "Nhân viên đã được " + status + " thành công.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
        }
        return "redirect:/admin/quan-ly-nhan-vien";
    }

    @PostMapping("/tao-moi")
    public String taoMoiNhanVien(
            @RequestParam String fullname,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu không khớp!");
            return "redirect:/admin/quan-ly-nhan-vien";
        }

        if (userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email đã tồn tại!");
            return "redirect:/admin/quan-ly-nhan-vien";
        }
        if (userRepository.existsByPhone(phone)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại đã tồn tại!");
            return "redirect:/admin/quan-ly-nhan-vien";
        }
        if (!isValidPassword(password)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Mật khẩu phải có ít nhất 6 ký tự, chứa chữ hoa, số và ký tự đặc biệt.");
            return "redirect:/admin/quan-ly-nhan-vien";
        }

        Users newUser = new Users();
        newUser.setFullname(fullname);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(Users.Role.STAFF);
        newUser.setEnabled(true);
        newUser.setCreatedAt(LocalDateTime.now());

        userRepository.save(newUser);

        redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản nhân viên thành công!");
        return "redirect:/admin/quan-ly-nhan-vien";
    }

    private boolean isValidPassword(String password) {
        String pattern = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$";
        return password.matches(pattern);
    }

    @PostMapping("/xoa")
    public String xoaNhanVien(@RequestParam("userId") Integer userId,
            RedirectAttributes redirectAttributes) {
        Optional<Users> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            userRepository.deleteById(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa nhân viên thành công.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy nhân viên để xóa.");
        }
        return "redirect:/admin/quan-ly-nhan-vien";
    }

}
