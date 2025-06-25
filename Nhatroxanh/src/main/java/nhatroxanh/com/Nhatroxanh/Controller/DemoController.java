package nhatroxanh.com.Nhatroxanh.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoController {

    @GetMapping("/chi-tiet")
    public String chitiet() {
        return "guest/chi-tiet";
    }

    @GetMapping("/nhan-vien/category")
    public String quanly() {
        return "staff/categoty";
    }

    @GetMapping("/phong-tro-fe")
    public String danhmuc() {
        return "guest/phong-tro";
    }

    @GetMapping("/bai-dang")
    public String baidang() {
        return "host/bai-dang-host";
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

    @GetMapping("/chu-tro/quan-ly-tro")
    public String phongtro() {
        return "host/phongtro";
    }

    @GetMapping("/chu-tro/tong-quan")
    public String tongquan() {
        return "host/tong-quan";
    }

    @GetMapping("/chu-tro/khach-thue")
    public String khachthue() {
        return "host/quan-ly-khach-thue";
    }

    @GetMapping("/chu-tro/dang-tin")
    public String dangtin() {
        return "host/bai-dang-host";
    }

    @GetMapping("/chu-tro/bai-dang")
    public String quanlyhopdong() {
        return "host/quan-ly-bai-dang";
    }

    @GetMapping("/chu-tro/chi-tiet-bai-dang")
    public String chitietbaidang() {
        return "host/chi-tiet-bai-dang";
    }

    @GetMapping("/chu-tro/chi-tiet-khach-thue")
    public String chitietkhachthue() {
        return "host/chi-tiet-khach-thue";
    }
    @GetMapping("/chu-tro/sua-bai-dang")
    public String chitiethopdong() {
        return "host/sua-bai-dang";
    }
    @GetMapping("/infor-chu-tro")
    public String inforchutro() {
        return "host/infor-chutro";
    }
}
