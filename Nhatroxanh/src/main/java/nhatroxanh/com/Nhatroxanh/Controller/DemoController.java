package nhatroxanh.com.Nhatroxanh.Controller;


import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentResponseDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;

import nhatroxanh.com.Nhatroxanh.Service.FavoritePostService;
import nhatroxanh.com.Nhatroxanh.Service.PaymentService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;


import java.util.*;


@Controller
public class DemoController {
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private FavoritePostService favoritePostService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RoomsService roomsService;

    @GetMapping("/tro-da-luu")
    public String savedPosts(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            try {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                Optional<Users> userOpt = userRepository.findById(userDetails.getUserId());
                
                if (userOpt.isPresent()) {
                    List<Post> favoritePosts = favoritePostService.getFavoritePostsByUser(userOpt.get());
                    model.addAttribute("favoritePosts", favoritePosts);
                }
            } catch (Exception e) {
                // Log error and continue with empty favorites
                System.err.println("Error loading favorite posts: " + e.getMessage());
            }
        }
        return "guest/tro-da-luu";
    }

    @GetMapping("/chi-tiet")
    public String chitiet() {
        return "guest/chi-tiet";
    }

    @GetMapping("/phong-tro-fe")
    public String danhmuc() {
        return "guest/phong-tro";
    }

    @GetMapping("/chu-tro/hop-dong")
    public String hopdong() {
        return "redirect:/api/contracts/form";
    }

    @GetMapping("/chu-tro/DS-hop-dong-host")
    public String contractsPage() {
        return "host/DS-hop-dong-host"; // Chỉ trả view, data load bằng AJAX
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
    @GetMapping("/khach-thue/thanh-toan")
    public String thanhToan() {
        return "guest/thanh-toan";
    }
    @GetMapping("/chu-tro/voucher")
    public String voucherHosst() {
        return "host/voucher-host";
    }
    @GetMapping("/notifications")
    public String notifications() {
        return "guest/chitiet-thongbao";
    }
    @GetMapping("/infor-chutro")
    public String chutro() {
        return "host/infor-chutro";
    }
}
