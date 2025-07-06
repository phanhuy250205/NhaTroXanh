package nhatroxanh.com.Nhatroxanh.Controller;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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



@Controller
public class DemoController {


    

     @Autowired
     private UserService userService;
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

    @GetMapping("/nhan-vien/thong-tin-tro")
    public String thongtintro() {
        return "staff/thong-tin-tro-staff";
    }

    @GetMapping("nhan-vien/chi-tiet-thong-tin-tro")
    public String detailthongtintro() {
        return "staff/detail-thong-tin-tro-staff";
    }

    @GetMapping("/chu-tro/hop-dong")
    public String hopdong() {
        
        return "redirect:/api/contracts/form";
    }

    @GetMapping("/chu-tro/DS-hop-dong-host")
    public String thuetra() {
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
        Contracts.Status.INACTIVE, 
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
    public String Thanhtoan() {
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

    // @GetMapping("/chu-tro/tong-quan")
    // public String tongquan() {
    //     return "host/tong-quan";
    // }

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

    // @GetMapping("/chu-tro/chi-tiet-bai-dang")
    // public String chitietbaidang() {
    //     return "host/chi-tiet-bai-dang";
    // }

     @GetMapping("/chu-tro/chi-tiet-khach-thue/{id}")
    public String chitietkhachthue(@PathVariable("id") Integer contractId, Model model) {
        try {
            TenantDetailDTO tenantDetail = tenantService.getTenantDetailByContractId(contractId);
            model.addAttribute("tenant", tenantDetail);
            return "host/chi-tiet-khach-thue"; // Tên file HTML chi tiết
        } catch (Exception e) {
            // Nếu không tìm thấy hợp đồng, chuyển hướng về trang danh sách và báo lỗi
            // (Bạn có thể tạo một trang lỗi riêng nếu muốn)
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
