package nhatroxanh.com.Nhatroxanh.Controller;

import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import nhatroxanh.com.Nhatroxanh.Service.FavoritePostService;
import nhatroxanh.com.Nhatroxanh.Service.PaymentService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantDetailDTO;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantRoomHistoryDTO;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantSummaryDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import nhatroxanh.com.Nhatroxanh.Service.TenantService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import org.springframework.web.bind.annotation.PathVariable;

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

    @GetMapping("/chu-tro/hop-dong/edit/{contractId}")
    @PreAuthorize("hasRole('OWNER')")
    public String editContract(
            @PathVariable Integer contractId,
            Authentication authentication,
            Model model) {
        try {
            // Có thể thêm logic kiểm tra quyền nếu cần
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Chuyển hướng đến endpoint edit của ContractController
            return "redirect:/api/contracts/edit/" + contractId;
        } catch (Exception e) {
            // Xử lý lỗi nếu cần
            model.addAttribute("error", "Không thể mở hợp đồng: " + e.getMessage());
            return "redirect:/chu-tro/DS-hop-dong-host"; // Quay lại trang danh sách nếu lỗi
        }
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
            @RequestParam(name = "hostelId", required = false) Integer selectedHostelId,
            @RequestParam(name = "status", required = false) Contracts.Status statusFilter) {

        Integer ownerId = loggedInUser.getUserId();

        Page<TenantInfoDTO> tenantPage = tenantService.getTenantsForOwner(
                ownerId, keyword, selectedHostelId, statusFilter, PageRequest.of(page, 10));

        List<Hostel> ownerHostels = tenantService.getHostelsForOwner(ownerId);
        Map<String, Long> stats = tenantService.getContractStatusStats(ownerId);

        System.out.println("📊 Stats map truyền ra view: " + stats);
        model.addAttribute("tenants", tenantPage.getContent());
        model.addAttribute("totalPages", tenantPage.getTotalPages());
        model.addAttribute("currentPage", tenantPage.getNumber());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedHostelId", selectedHostelId);
        model.addAttribute("hostels", ownerHostels);
        model.addAttribute("selectedStatus", statusFilter);
        model.addAttribute("isHistoryPage", true);
        model.addAttribute("contractStats", stats);

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
                model.addAttribute("totalPages",
                        (int) Math.ceil((double) paymentService.getPaymentsByOwnerId(ownerId).size() / 8));

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

    @GetMapping("/chu-tro/quan-ly-tro")
    public String phongtro() {
        return "host/phongtro";
    }

    @GetMapping("/chu-tro/khach-thue")
    public String showTenantManagementPage(
            Model model,
            @AuthenticationPrincipal CustomUserDetails loggedInUser,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "keyword", required = false) String keyword) {

        Integer ownerId = loggedInUser.getUserId();

        Page<TenantSummaryDTO> tenantPage = tenantService.getTenantSummaryForOwner(
                ownerId,
                keyword,
                PageRequest.of(page, 10));

        model.addAttribute("tenants", tenantPage.getContent());
        model.addAttribute("totalPages", tenantPage.getTotalPages());
        model.addAttribute("currentPage", tenantPage.getNumber());
        model.addAttribute("keyword", keyword);

        return "host/quan-ly-khach-thue";
    }

    // @GetMapping("/chu-tro/dang-tin")
    // public String dangtin() {
    // return "host/bai-dang-host";
    // }

    // @GetMapping("/chu-tro/bai-dang")
    // public String quanlyhopdong() {
    // return "host/quan-ly-bai-dang";
    // }

    // @GetMapping("/chu-tro/Qlthue-tra")
    // public String chitietbaidang() {
    // return "guest/quan-ly-thue-tra";
    // }

    @GetMapping("/chu-tro/chi-tiet-khach-thue")
    public String chitietkhachthue() {
        return "host/chi-tiet-khach-thue";
    }

    // @GetMapping("/khach-thue/quan-ly-thue-tra")
    // public String quanLyThueTra() {
    // return "guest/quan-ly-thue-tra";
    // }

    // @GetMapping("/khach-thue/chitiet-phongthue")
    // public String chiTietPhongThue() {
    // return "guest/chitiet-phongthue";
    // }
    @GetMapping("/khach-thue/thanh-toan")
    public String thanhToan() {
        return "guest/thanh-toan";
    }

    @GetMapping("/chu-tro/chi-tiet-khach-thue/{id}")
    public String chitietkhachthue(@PathVariable("id") Integer userId, Model model) {
        try {
            TenantDetailDTO tenantDetail = tenantService.getTenantDetailByUserId(userId); // 🔁 dùng userId
            model.addAttribute("tenant", tenantDetail);

            // Lấy lịch sử thuê trọ
            List<TenantRoomHistoryDTO> historyList = tenantService.getTenantRentalHistory(userId);
            model.addAttribute("rentalHistory", historyList);

            return "host/chi-tiet-khach-thue";
        } catch (Exception e) {
            return "redirect:/chu-tro/khach-thue";
        }
    }

    @PostMapping("/chu-tro/khach-thue/kich-hoat")
    public String toggleStatus(@RequestParam("userId") Integer userId,
            @RequestParam("contractId") Integer contractId,
            RedirectAttributes redirectAttributes) {
        Users tenant = userRepository.findById(userId).orElse(null);
        if (tenant == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
            return "redirect:/chu-tro/khach-thue";
        }

        tenant.setEnabled(!tenant.isEnabled());
        userRepository.save(tenant);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công.");
        // ✅ chuyển hướng lại đúng trang chi tiết khách thuê
        return "redirect:/chu-tro/chi-tiet-khach-thue/" + contractId;
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "guest/chitiet-thongbao";
    }

    @GetMapping("/infor-chutro")
    public String chutro() {
        return "host/infor-chutro";
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
    // return "host/sua-bai-dang";

}
