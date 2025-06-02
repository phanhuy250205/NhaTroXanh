package nhatroxanh.com.Nhatroxanh.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/trang-chu")
    public String home() {
        return "index";
    }
    @GetMapping("/demo")
    public String demo() {
        return "guest/demo";
    }

    @GetMapping("/chi-tiet")
    public String chitiet() {
        return "guest/chi-tiet";
    }

    @GetMapping("/quanly")
    public String quanly() {
        return "staff/categoty";
    }
    @GetMapping("/phong-tro")
    public String danhmuc() {
        return "guest/phong-tro";
    }
}
