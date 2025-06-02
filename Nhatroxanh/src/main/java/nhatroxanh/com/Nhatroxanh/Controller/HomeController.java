package nhatroxanh.com.Nhatroxanh.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/trang-chu")
    public String home() {
        return "index";
    }

    @GetMapping("/chi-tiet")
    public String chitiet() {
        return "guest/chi-tiet";
    }
}
