package nhatroxanh.com.Nhatroxanh.Controller.web;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RoomManagementController {

    @Autowired
    private HostelService hostelService;

    // Hiển thị danh sách khu trọ
    @GetMapping("/chu-tro/thong-tin-tro")
    public String hostThongTinTro(Model model) {
        model.addAttribute("hostels", hostelService.findAllHostels());
        return "host/thongtintro";
    }

    // Hiển thị form thêm/sửa khu trọ
    @GetMapping("/chu-tro/them-khu-tro")
    public String themKhuTro(Model model, @RequestParam(value = "id", required = false) Integer id) {
        Hostel hostel = id != null ? hostelService.findHostelById(id).orElse(new Hostel()) : new Hostel();
        model.addAttribute("hostel", hostel);
        return "host/themkhutro";
    }

    // Lưu khu trọ
    @PostMapping("/chu-tro/save-khu-tro")
    public String saveKhuTro(@ModelAttribute Hostel hostel) {
        hostelService.saveHostel(hostel);
        return "redirect:/chu-tro/thong-tin-tro";
    }

    // Xóa khu trọ
    @PostMapping("/chu-tro/delete-khu-tro/{id}")
    public String deleteKhuTro(@PathVariable Integer id) {
        hostelService.deleteHostel(id);
        return "redirect:/chu-tro/thong-tin-tro";
    }
}