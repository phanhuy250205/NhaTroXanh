package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/nhan-vien")
public class OwnerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/chu-tro")
    public String listOwners(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter) {

        Page<Users> owners = userService.getFilteredOwners(search, statusFilter, page, size);

        model.addAttribute("owners", owners);
        model.addAttribute("currentPage", page);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);

        return "staff/chu-tro";
    }

    @GetMapping("/chi-tiet-chu-tro/{id}")
    public String showOwnerDetail(@PathVariable("id") int id, Model model) {
        Optional<Users> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return "redirect:/nhan-vien/chu-tro";
        }

        Users user = optionalUser.get();
        List<Hostel> hostels = hostelRepository.findByOwner(user);
        UserCccd userCccd = userCccdRepository.findByUser(user);

        model.addAttribute("user", user);
        model.addAttribute("hostels", hostels);
        model.addAttribute("cccd", userCccd);

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

}
