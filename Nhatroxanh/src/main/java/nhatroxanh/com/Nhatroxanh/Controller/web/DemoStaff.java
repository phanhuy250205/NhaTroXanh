package nhatroxanh.com.Nhatroxanh.Controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class DemoStaff {

    @GetMapping("/nhan-vien/bai-dang")
    public String baidang(Model model) {
        return "staff/bai-dang"; 
    }
    @GetMapping("/nhan-vien/khuyen-mai")
    public String khuyenmai(Model model) {
        return "staff/khuyen-mai"; 
    }
    @GetMapping("/nhan-vien/khieu-nai")
    public String khieunai(Model model) {
        return "staff/khieu-nai"; 
    }
    @GetMapping("/nhan-vien/chi-tiet-khieu-nai")
    public String chitietkhieunai(Model model) {
        return "staff/chitiet-khieunai"; 
    }

    @GetMapping("/nhan-vien/chu-tro")
    public String chutro(Model model) {
        return "staff/chu-tro"; 
    }
    @GetMapping("/nhan-vien/chi-tiet-chu-tro")
    public String chitietchutro(Model model) {
        return "staff/chitiet-chutro"; 
    }

    @GetMapping("/nhan-vien/khach-thue")
    public String khachthue(Model model) {
        return "staff/khach-thue"; 
    }
       @GetMapping("/nhan-vien/profile")
    public String profile(Model model) {
        return "staff/profile"; 
    }
}
