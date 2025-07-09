package nhatroxanh.com.Nhatroxanh.Controller;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractListDto;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantDetailDTO;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import nhatroxanh.com.Nhatroxanh.Service.TenantService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

     
     @Autowired
     private TenantService tenantService;
     @Autowired
     private HostelRepository hostelRepository;
     @Autowired
    private ContractService contractService;
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
        return "host/DS-hop-dong-host"; 
    }

    @GetMapping("/chu-tro/lich-su-thue")
public String showRentalHistory(
    Model model,
    @AuthenticationPrincipal CustomUserDetails loggedInUser,
    @RequestParam(name = "page", defaultValue = "0") int page,
    @RequestParam(name = "keyword", required = false) String keyword,
    @RequestParam(name = "hostelId", required = false) Integer selectedHostelId) {

    Integer ownerId = loggedInUser.getUserId();

    Page<TenantInfoDTO> tenantPage = tenantService.getTenantsForOwner(
        ownerId,
        keyword,
        selectedHostelId,
        Contracts.Status.EXPIRED, 
        PageRequest.of(page, 10)
    );

    List<Hostel> ownerHostels = tenantService.getHostelsForOwner(ownerId);

    model.addAttribute("tenants", tenantPage.getContent());
    model.addAttribute("totalPages", tenantPage.getTotalPages());
    model.addAttribute("currentPage", tenantPage.getNumber());
    model.addAttribute("keyword", keyword);
    model.addAttribute("selectedHostelId", selectedHostelId);
    model.addAttribute("hostels", ownerHostels);
    model.addAttribute("isHistoryPage", true); // optional

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


    @GetMapping("/chu-tro/quan-ly-tro")
    public String phongtro() {
        return "host/phongtro";
    }

    @GetMapping("/chu-tro/khach-thue")
public String showTenantManagementPage(
        Model model,
        @AuthenticationPrincipal CustomUserDetails loggedInUser,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "hostelId", required = false) Integer selectedHostelId) {

    Integer ownerId = loggedInUser.getUserId();

    // Lấy danh sách khách đang thuê
    Page<TenantInfoDTO> tenantPage = tenantService.getTenantsForOwner(
        ownerId,
        keyword,
        selectedHostelId,
        Contracts.Status.ACTIVE, 
        PageRequest.of(page, 10)
    );

    List<Hostel> ownerHostels = tenantService.getHostelsForOwner(ownerId);

    model.addAttribute("tenants", tenantPage.getContent());
    model.addAttribute("totalPages", tenantPage.getTotalPages());
    model.addAttribute("currentPage", tenantPage.getNumber());
    model.addAttribute("keyword", keyword);
    model.addAttribute("selectedHostelId", selectedHostelId);
    model.addAttribute("hostels", ownerHostels);
    model.addAttribute("selectedStatus", true); // nếu muốn gửi sang để giữ trạng thái filter

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

     @GetMapping("/chu-tro/chi-tiet-khach-thue/{id}")
    public String chitietkhachthue(@PathVariable("id") Integer contractId, Model model) {
        try {
            TenantDetailDTO tenantDetail = tenantService.getTenantDetailByContractId(contractId);
            model.addAttribute("tenant", tenantDetail);
            return "host/chi-tiet-khach-thue"; // Tên file HTML chi tiết
        } catch (Exception e) {
            return "redirect:/chu-tro/khach-thue";
        }
        
    }

    private Integer getCurrentOwnerId() {
    String email = org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication().getName();

    return userService.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"))
        .getUserId();
}
    
    
    // @GetMapping("/chu-tro/sua-bai-dang")
    // public String chitiethopdong() {
    //     return "host/sua-bai-dang";
    // }
}
