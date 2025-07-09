package nhatroxanh.com.Nhatroxanh.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import nhatroxanh.com.Nhatroxanh.Service.UserService;




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
    // @GetMapping("/chu-tro/sua-bai-dang")
    // public String chitiethopdong() {
    //     return "host/sua-bai-dang";
    // }
}
