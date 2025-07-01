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
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
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
    public String LsThuetra() {
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
    public String khachthue(Model model,
                                @RequestParam(defaultValue = "") String keyword,
                                @RequestParam(required = false) Integer hostelId,
                                @RequestParam(required = false) Boolean status,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {
        
        Page<TenantInfoDTO> tenantPage = tenantService.findAllForTesting(PageRequest.of(page, size));
        List<Hostel> hostels = hostelRepository.findAll();

        model.addAttribute("tenants", tenantPage.getContent());
        model.addAttribute("hostels", hostels);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tenantPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedHostelId", hostelId);
        model.addAttribute("selectedStatus", status);

        return "host/quan-ly-khach-thue"; // Tên file HTML danh sách
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
    
    // @GetMapping("/chu-tro/sua-bai-dang")
    // public String chitiethopdong() {
    //     return "host/sua-bai-dang";
    // }
}
