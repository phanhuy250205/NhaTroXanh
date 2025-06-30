package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.Dto.HostelDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Controller
public class HostelController {

    @Autowired
    private HostelService hostelService;

    @GetMapping("/chu-tro/thong-tin-tro")
    public String hostthongtintro(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Integer ownerId = userDetails.getUser().getUserId();
            List<Hostel> hostels = hostelService.getHostelsByOwnerId(ownerId);
            model.addAttribute("hostels", hostels);
            return "host/thongtintro";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải danh sách khu trọ: " + e.getMessage());
            return "host/thongtintro";
        }
    }
    @GetMapping("chu-tro/them-khu-tro")
    public String themkhuatro(Model model) {
        model.addAttribute("hostel", new HostelDTO());
        return "host/themkhutro";
    }
    @PostMapping("/chu-tro/them-khu-tro")
    public String saveHostel(@ModelAttribute HostelDTO hostelDTO,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            hostelDTO.setOwnerId(userDetails.getUser().getUserId());
            hostelService.createHostel(hostelDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm khu trọ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm khu trọ: " + e.getMessage());
        }
        return "redirect:/chu-tro/thong-tin-tro";
    }


}