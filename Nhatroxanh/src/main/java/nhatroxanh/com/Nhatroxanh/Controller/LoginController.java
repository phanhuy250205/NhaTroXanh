package nhatroxanh.com.Nhatroxanh.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

@Controller
public class LoginController {
    @Autowired
    private UserService userService;
    @GetMapping("/dang-nhap-chu-tro")
    public String loginHost() {
        return "auth/login-host";
    }
    @GetMapping("/dang-ky-chu-tro")
    public String registerHost() {
        return "auth/register-host";
    }
    @GetMapping("/dang-ky-chi-tiet")
    public String showDetailedForm(@RequestParam("userId") Integer userId, Model model) {
        
        // --- PHẦN SỬA ĐỔI ---
        // 1. Lấy thông tin người dùng từ database bằng userId
        Users user = userService.getById(userId); // Giả sử getById() đã có trong service

        // 2. Thêm toàn bộ đối tượng 'user' vào model
        model.addAttribute("user", user); 
        
        // 3. Giữ lại userId để dùng cho thẻ input hidden
        model.addAttribute("userId", userId);
        
        // Tên file của bạn là "host/infor-chutro" hoặc "auth/thong-tin-chu-tro"
        return "host/infor-chutro"; 
    }

    // Xử lý submit form thông tin chi tiết
    @PostMapping("/hoan-tat-dang-ky")
    public String processDetailedForm(
            @RequestParam("userId") Integer userId,
            @RequestParam("gender") Boolean gender,
            @RequestParam("cccdNumber") String cccdNumber,
            @RequestParam("issueDate") String issueDate, 
            @RequestParam("issuePlace") String issuePlace,
            @RequestParam("address") String address,
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.completeOwnerRegistration(userId, gender, cccdNumber, issueDate, issuePlace, address, frontImage, backImage);
            redirectAttributes.addFlashAttribute("successMessage", "Hoàn tất đăng ký! Yêu cầu của bạn đang chờ duyệt.");
            return "redirect:/dang-nhap-chu-tro";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/dang-ky-chi-tiet?userId=" + userId;
        }
    }
}
