package nhatroxanh.com.Nhatroxanh.Controller.web;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.Ward;
import nhatroxanh.com.Nhatroxanh.Repository.WardRepository;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chu-tro")
public class RoomManagementController {

    @Autowired
    private HostelService hostelService;

    @GetMapping("/thong-tin-tro")
    public String thongTinTro(Model model) {
        // Lấy danh sách tất cả các khu trọ
        model.addAttribute("hostels", hostelService.findAllHostels());
        return "host/thongtintro"; // Giả định template là thongtintro.html
    }

    @GetMapping("/them-khu-tro")
    public String themKhuTro(Model model, @RequestParam(value = "id", required = false) Integer id) {
        Hostel hostel = id != null ? hostelService.findHostelById(id).orElse(new Hostel()) : new Hostel();
        if (hostel.getAddress() == null) {
            hostel.setAddress(new Address()); // Khởi tạo Address nếu null
        }
        model.addAttribute("hostel", hostel);
        return "host/themkhutro";
    }

    @PostMapping("/save-khu-tro")
    public String saveKhuTro(@ModelAttribute Hostel hostel,
                             @RequestParam("provinceHost") Integer provinceId,
                             @RequestParam("districtHost") Integer districtId,
                             @RequestParam("wardHost") Integer wardId,
                             @RequestParam("streetHost") String street,
                             @RequestParam("houseNumberHost") String houseNumber,
                             WardRepository wardRepository) {
        Address address = hostel.getAddress();
        if (address == null) {
            address = new Address();
            hostel.setAddress(address);
        }
        Ward ward = wardRepository.findById(wardId).orElseThrow(() -> new RuntimeException("Ward not found"));
        address.setWard(ward);
        address.setStreet(houseNumber + ", " + street);
        hostelService.saveHostel(hostel);
        return "redirect:/chu-tro/thong-tin-tro";
    }

    @GetMapping("/edit-khu-tro")
    public String editKhuTro(@RequestParam Integer id, Model model) {
        Hostel hostel = hostelService.findHostelById(id).orElseThrow(() -> new RuntimeException("Hostel not found"));
        model.addAttribute("hostel", hostel);
        return "host/themkhutro"; // Sử dụng cùng template cho thêm và sửa
    }

    @GetMapping("/delete-khu-tro")
    public String deleteKhuTro(@RequestParam Integer id) {
        hostelService.deleteHostelById(id);
        return "redirect:/chu-tro/thong-tin-tro";
    }
}