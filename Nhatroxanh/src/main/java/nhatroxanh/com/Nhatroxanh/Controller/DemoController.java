package nhatroxanh.com.Nhatroxanh.Controller;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractListDto;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

import java.util.List;


@Controller
public class DemoController {
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    private ContractService contractService;
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
    public String contractsPage() {
        return "host/DS-hop-dong-host"; // Chỉ trả view, data load bằng AJAX
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
}
