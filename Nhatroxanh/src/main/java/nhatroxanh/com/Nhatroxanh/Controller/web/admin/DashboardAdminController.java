package nhatroxanh.com.Nhatroxanh.Controller.web.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import nhatroxanh.com.Nhatroxanh.Service.DashboardAdminService;
import nhatroxanh.com.Nhatroxanh.Service.DashboardAdminService.DashboardStats;

@Controller
@RequestMapping("/admin/thong-ke")
public class DashboardAdminController {
    @Autowired
    private DashboardAdminService dashboardService;
    @GetMapping
    public String thongke(Model model) {
        DashboardStats stats = dashboardService.getDashboardStatistics();
        model.addAttribute("stats", stats);
        return "admin/thong-ke";
    }
}
