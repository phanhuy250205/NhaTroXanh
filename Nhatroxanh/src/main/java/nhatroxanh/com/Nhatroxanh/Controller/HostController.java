package nhatroxanh.com.Nhatroxanh.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HostController {
    @GetMapping("/thong-tin-tro-host")
    public String thongTinTroHost() {
        return "host/thongtintro";
    }
    
    @GetMapping("/phong-tro-host")
    public String phongTroHost() {
        return "host/phongtro";
    }

    @GetMapping("/them-khu-tro-host")
    public String themKhuTroHost() {
        return "host/themkhutro";
    }

    @GetMapping("/nap-tien")
    public String napTien() {
        return "redirect:/host/wallet/nap-tien";
    }

    @GetMapping("/rut-tien")
    public String rutTien() {
        return "redirect:/host/wallet/rut-tien";
    }
}
