package nhatroxanh.com.Nhatroxanh.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoController {
    @GetMapping("/trang-chu")
    public String home() {
        return "index";
    }
    @GetMapping("/chi-tiet")
    public String chitiet() {
        return "guest/chi-tiet";
    }

    @GetMapping("/category")
    public String quanly() {
        return "staff/categoty";
    }
   
    @GetMapping("/bai-dang")
    public String baidang() {
        return "host/bai-dang-host";
    }

    @GetMapping("/nhan-vien/thong-tin-tro")
    public String thongtintro() {
        return "staff/thong-tin-tro-staff";
    }
    @GetMapping("detail-thong-tin-tro")
    public String detailthongtintro() {
        return "staff/detail-thong-tin-tro-staff";
    }
    @GetMapping("/chu-tro/hop-dong")
    public String hopdong() {
        return "host/hop-dong-host";
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
     @GetMapping("/chu-tro/profile-host")
    public String profilehost() {
        return "host/profile-host";
    }
    
    @GetMapping("/chu-tro/Dk")
    public String dangkychutro() {
        return "auth/register-host";
    }
    @GetMapping("/chu-tro/Dn")
    public String dangnhapchutro() {
        return "auth/login-host";
    }
}
