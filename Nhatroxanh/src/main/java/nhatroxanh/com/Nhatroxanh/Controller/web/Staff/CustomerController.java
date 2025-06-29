package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/nhan-vien")
public class CustomerController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractsRepository contractRepository;

    @GetMapping("/khach-thue")
    public String getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {
        Page<Users> customerPage = userService.getAllCustomers(page, size);
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customerPage.getTotalPages());
        model.addAttribute("pageSize", size);
        return "Staff/khach-thue";
    }

    @GetMapping("/chi-tiet-khach-thue/{id}")
    public String getCustomerDetail(@PathVariable("id") Integer userId, Model model) {
        Optional<Users> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return "redirect:/nhan-vien/khach-thue?error=notfound";
        }
        Users customer = userOpt.get();
        UserCccd cccd = customer.getUserCccd();
        customer.setUserCccd(cccd);
        Contracts latestContract = contractRepository
                .findTopByUserOrderByStartDateDesc(customer)
                .orElse(null);
        model.addAttribute("customer", customer);
        model.addAttribute("contract", latestContract);

        return "Staff/chi-tiet-khach-thue";
    }

    @PostMapping("/toggle-enable")
    public String toggleEnable(@RequestParam("userId") Integer userId, RedirectAttributes redirectAttributes) {
        Optional<Users> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            user.setEnabled(!user.isEnabled()); 
            userRepository.save(user);
            String status = user.isEnabled() ? "mở khóa" : "vô hiệu hóa";
            redirectAttributes.addFlashAttribute("successMessage", "Khách thuê đã được " + status + " thành công.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
        }
        return "redirect:/nhan-vien/chi-tiet-khach-thue/" + userId;
    }

}
