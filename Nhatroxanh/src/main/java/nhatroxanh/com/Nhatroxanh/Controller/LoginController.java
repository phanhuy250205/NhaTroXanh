package nhatroxanh.com.Nhatroxanh.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping("/dang-nhap-chu-tro")
    public String loginHost() {
        return "auth/login-host";
    }
    @GetMapping("/dang-ky-chu-tro")
    public String registerHost() {
        return "auth/register-host";
    }

    
}
