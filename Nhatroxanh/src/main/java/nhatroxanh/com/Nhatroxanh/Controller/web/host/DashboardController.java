package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    @GetMapping("/chu-tro/tong-quan")
    public String tongquan() {
        return "host/tong-quan";
    }
}
