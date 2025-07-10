package nhatroxanh.com.Nhatroxanh.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentResponseDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.FavoritePostService;
import nhatroxanh.com.Nhatroxanh.Service.PaymentService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class DemoController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RoomsService roomsService;
     @Autowired
     private UserService userService;
     
    @GetMapping("/chi-tiet")
    public String chitiet() {
        return "guest/chi-tiet";
    }

    @GetMapping("/phong-tro-fe")
    public String danhmuc() {
        return "guest/phong-tro";
    }

    // @GetMapping("/nhan-vien/thong-tin-tro")
    // public String thongtintro() {
    //     return "staff/thong-tin-tro-staff";
    // }

    // @GetMapping("nhan-vien/chi-tiet-thong-tin-tro")
    // public String detailthongtintro() {
    //     return "staff/detail-thong-tin-tro-staff";
    // }

    @GetMapping("/chu-tro/hop-dong")
    public String hopdong() {
        return "redirect:/api/contracts/form";
    }

    @GetMapping("/chu-tro/DS-hop-dong-host")
    public String thuetra() {
        return "host/DS-hop-dong-host";
    }

    @GetMapping("/chu-tro/lich-su-thue")
    public String LsThuetra() {
        return "host/LS-thue-tra-host";
    }

    @GetMapping("/chu-tro/thanh-toan")
    public String Thanhtoan(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            try {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                Integer ownerId = userDetails.getUserId();
                
                // Lấy thống kê payments
                Map<String, Object> statistics = paymentService.getPaymentStatistics(ownerId);
                model.addAttribute("paymentStats", statistics);
                
                // Lấy 8 payments mới nhất thay vì tất cả payments
                List<PaymentResponseDto> recentPayments = paymentService.getRecentPaymentsByOwnerId(ownerId);
                model.addAttribute("payments", recentPayments);
                
                // Thêm thông tin phân trang
                model.addAttribute("currentPage", 0);
                model.addAttribute("pageSize", 8);
                model.addAttribute("totalPayments", paymentService.getPaymentsByOwnerId(ownerId).size());
                model.addAttribute("totalPages", (int) Math.ceil((double) paymentService.getPaymentsByOwnerId(ownerId).size() / 8));
                
                // Lấy danh sách contracts có thể tạo payment
                List<Map<String, Object>> availableContracts = paymentService.getAvailableContractsForPayment(ownerId);
                model.addAttribute("availableContracts", availableContracts);
                
                // Lấy danh sách phòng cho filter
                model.addAttribute("rooms", roomsService.getRoomsByOwnerId(ownerId));
                
            } catch (Exception e) {
                // Log error và tiếp tục với dữ liệu mặc định
                System.err.println("Error loading payment data: " + e.getMessage());
            }
        }
        return "host/QL-thanh-toan-host";
    }

    @GetMapping("/chu-tro/gia-hang-tra-phong")
    public String giahangtraphong() {
        return "host/gia-han-tra-phong-host";
    }

    @GetMapping("/chu-tro/danh-gia")
    public String danhgia() {
        return "host/QL-danh-gia-host";
    }

    @GetMapping("/chu-tro/quan-ly-tro")
    public String phongtro() {
        return "host/phongtro";
    }

    @GetMapping("/chu-tro/khach-thue")
    public String khachthue(Model model) {
        return "host/quan-ly-khach-thue";
    }

    // @GetMapping("/chu-tro/dang-tin")
    // public String dangtin() {
    //     return "host/bai-dang-host";
    // }

    // @GetMapping("/chu-tro/bai-dang")
    // public String quanlyhopdong() {
    //     return "host/quan-ly-bai-dang";
    // }

     @GetMapping("/chu-tro/Qlthue-tra")
     public String chitietbaidang() {
         return "guest/quan-ly-thue-tra";
     }

    @GetMapping("/chu-tro/chi-tiet-khach-thue")
    public String chitietkhachthue() {
        return "host/chi-tiet-khach-thue";
    }
    @GetMapping("/khach-thue/quan-ly-thue-tra")
    public String quanLyThueTra() {
        return "guest/quan-ly-thue-tra";
    }
    @GetMapping("/khach-thue/chitiet-phongthue")
    public String chiTietPhongThue() {
        return "guest/chitiet-phongthue";
    }

    @GetMapping("/chu-tro/quan-ly-su-co")
    public String quanLySuCoCT() {
        return "host/quan-ly-su-co";
    }    
    
    // @GetMapping("/profile-khach-thue")
    // public String profileKhachThue() {
    //     return "guest/profile";
    // }  
}
