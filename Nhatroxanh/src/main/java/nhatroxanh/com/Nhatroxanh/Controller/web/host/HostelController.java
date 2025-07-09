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
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import nhatroxanh.com.Nhatroxanh.Service.Impl.HostelServiceImpl;

import java.util.HashMap;
import java.util.List;


@Controller
public class HostelController {

    @Autowired
    private HostelServiceImpl hostelServiceImpl;

    @Autowired
    private HostelService hostelService;

    @Autowired
    private RoomsService roomsService;

    @GetMapping("/chu-tro/thong-tin-tro")
    public String hostthongtintro(Model model, @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam(value = "keyword", required = false) String keyword) {
       try {
        Integer ownerId = userDetails.getUser().getUserId();
        List<Hostel> hostels;

        if (keyword != null && !keyword.trim().isEmpty()) {
            hostels = hostelService.searchHostelsByOwnerIdAndName(ownerId, keyword);
        } else {
            hostels = hostelService.getHostelsByOwnerId(ownerId);
        }

        model.addAttribute("hostels", hostels);
        model.addAttribute("keyword", keyword); // giữ lại giá trị ô tìm kiếm
        return "host/thongtintro";

    } catch (Exception e) {
        model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải danh sách khu trọ: " + e.getMessage());
        return "host/thongtintro";
    }
    }

    @GetMapping("/chu-tro/them-khu-tro")
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
            System.out.println("Received: provinceName=" + provinceName + ", districtName=" + districtName + ", wardName=" + wardName + ", street=" + street + ", houseNumber=" + houseNumber);
            hostelDTO.setOwnerId(userDetails.getUser().getUserId());
            hostelDTO.setProvinceCode(provinceCode);
            hostelDTO.setProvinceName(provinceName);
            hostelDTO.setDistrictCode(districtCode);
            hostelDTO.setDistrictName(districtName);
            hostelDTO.setWardCode(wardCode);
            hostelDTO.setWardName(wardName);
            hostelDTO.setStreet(street);
            hostelDTO.setHouseNumber(houseNumber);

            // Debug trước khi gọi setAddressFromComponents
            System.out.println("Before setAddressFromComponents: " + hostelDTO.toString());

            hostelDTO.getCombinedAddress();

            // Debug sau khi gọi setAddressFromComponents
            System.out.println("After setAddressFromComponents: " + hostelDTO.getAddress());

            hostelServiceImpl.createHostel(hostelDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm khu trọ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm khu trọ: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/chu-tro/thong-tin-tro";
    }

    @GetMapping("/chu-tro/cap-nhat-khu-tro")
    public String editHostel(@RequestParam("id") Integer hostelId, Model model) {
        try {
            Hostel hostel = hostelService.getHostelById(hostelId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khu trọ"));
            HostelDTO hostelDTO = new HostelDTO();
            hostelDTO.setHostelId(hostel.getHostelId());
            hostelDTO.setName(hostel.getName());
            hostelDTO.setDescription(hostel.getDescription());
            hostelDTO.setStatus(hostel.getStatus());
            hostelDTO.setRoomNumber(hostel.getRoom_number());
            hostelDTO.setCreatedAt(hostel.getCreatedAt());

            // Lấy địa chỉ từ database và phân tách
            String address = hostel.getAddress() != null ? hostel.getAddress().getStreet() : "";
            hostelDTO.parseAddress(address);

            if (hostel.getAddress() != null && hostel.getAddress().getWard() != null) {
                hostelDTO.setWardCode(hostel.getAddress().getWard().getCode());
                hostelDTO.setWardName(hostel.getAddress().getWard().getName());
                hostelDTO.setDistrictCode(hostel.getAddress().getWard().getDistrict().getCode());
                hostelDTO.setDistrictName(hostel.getAddress().getWard().getDistrict().getName());
                hostelDTO.setProvinceCode(hostel.getAddress().getWard().getDistrict().getProvince().getCode());
                hostelDTO.setProvinceName(hostel.getAddress().getWard().getDistrict().getProvince().getName());
            }

            model.addAttribute("hostel", hostelDTO);
            return "host/themkhutro";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Có lỗi khi tải thông tin khu trọ: " + e.getMessage());
            return "host/thongtintro";
        }
    }

    @PostMapping("/chu-tro/cap-nhat-khu-tro")
    public String updateHostel(@ModelAttribute HostelDTO hostelDTO,
                              @RequestParam("province") String provinceCode,
                              @RequestParam("provinceName") String provinceName,
                              @RequestParam("district") String districtCode,
                              @RequestParam("districtName") String districtName,
                              @RequestParam("ward") String wardCode,
                              @RequestParam("wardName") String wardName,
                              @RequestParam("street") String street,
                              @RequestParam("houseNumber") String houseNumber,
                              RedirectAttributes redirectAttributes) {
        try {
            hostelDTO.setProvinceCode(provinceCode);
            hostelDTO.setProvinceName(provinceName);
            hostelDTO.setDistrictCode(districtCode);
            hostelDTO.setDistrictName(districtName);
            hostelDTO.setWardCode(wardCode);
            hostelDTO.setWardName(wardName);
            hostelDTO.setStreet(street);
            hostelDTO.setHouseNumber(houseNumber);

            hostelDTO.getCombinedAddress();

            hostelServiceImpl.updateHostel(hostelDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khu trọ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật khu trọ: " + e.getMessage());
            e.printStackTrace();
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