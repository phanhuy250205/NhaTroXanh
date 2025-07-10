package nhatroxanh.com.Nhatroxanh.Controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoAdmin {
//  @GetMapping("/admin/quan-ly-nhan-vien")
//     public String nhanvien(Model model) {
//         return "admin/quan-ly-nhan-vien"; 
//     }
     @GetMapping("/admin/thong-ke")
    public String thongke(Model model) {
        return "admin/thong-ke"; 
    }
    //  @GetMapping("/admin/profile")
    // public String profile(Model model) {
    //     return "admin/profile"; 
    // }
}
