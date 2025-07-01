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
import nhatroxanh.com.Nhatroxanh.Service.Impl.HostelServiceImpl;

import java.util.HashMap;
import java.util.List;



@Controller
public class HostelController {

    @Autowired
    private HostelServiceImpl hostelServiceImpl;

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
                            @RequestParam("province") String provinceCode,
                            @RequestParam("provinceName") String provinceName,
                            @RequestParam("district") String districtCode,
                            @RequestParam("districtName") String districtName,
                            @RequestParam("ward") String wardCode,
                            @RequestParam("wardName") String wardName,
                            @RequestParam("street") String street,
                            @RequestParam("houseNumber") String houseNumber,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            hostelDTO.setOwnerId(userDetails.getUser().getUserId());
            hostelDTO.setStreet(street);
            hostelDTO.setHouseNumber(houseNumber);
            hostelDTO.setProvinceCode(provinceCode);
            hostelDTO.setProvinceName(provinceName);
            hostelDTO.setDistrictCode(districtCode);
            hostelDTO.setDistrictName(districtName);
            hostelDTO.setWardCode(wardCode);
            hostelDTO.setWardName(wardName);

            hostelServiceImpl.createHostel(hostelDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm khu trọ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm khu trọ: " + e.getMessage());
        }
        return "redirect:/chu-tro/thong-tin-tro";
    }

    @PostMapping("/chu-tro/xoa-khu-tro")
    public String deleteHostel(@RequestParam("hostelId") Integer hostelId, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("Deleting hostel with ID: " + hostelId);
            hostelService.deleteHostel(hostelId);
            System.out.println("Hostel deleted successfully");
            redirectAttributes.addFlashAttribute("successMessage", "Xóa khu trọ thành công!");
        } catch (Exception e) {
            System.out.println("Error deleting hostel: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa khu trọ: " + e.getMessage());
        }
        return "redirect:/chu-tro/thong-tin-tro";
    }
}