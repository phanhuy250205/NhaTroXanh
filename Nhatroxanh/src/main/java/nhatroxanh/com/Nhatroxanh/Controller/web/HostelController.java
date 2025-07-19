package nhatroxanh.com.Nhatroxanh.Controller.web;

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

    @GetMapping("/chu-tro/them-khu-tro")
    public String themkhutro(@RequestParam(required = false) Integer id, Model model) {
        try {
            HostelDTO hostelDTO = new HostelDTO();
            boolean isEdit = false;

            if (id != null) {
                Optional<Hostel> hostelOpt = hostelService.getHostelById(id);
                if (hostelOpt.isPresent()) {
                    Hostel hostel = hostelOpt.get();
                    hostelDTO.setHostelId(hostel.getHostelId());
                    hostelDTO.setName(hostel.getName());
                    hostelDTO.setDescription(hostel.getDescription());
                    hostelDTO.setStatus(hostel.getStatus());
                    hostelDTO.setRoomNumber(hostel.getRoom_number());
                    if (hostel.getAddress() != null && hostel.getAddress().getWard() != null) {
                        hostelDTO.setWard(String.valueOf(hostel.getAddress().getWard().getId()));
                        if (hostel.getAddress().getWard().getDistrict() != null) {
                            hostelDTO.setDistrict(String.valueOf(hostel.getAddress().getWard().getDistrict().getId()));
                            if (hostel.getAddress().getWard().getDistrict().getProvince() != null) {
                                hostelDTO.setProvince(String.valueOf(hostel.getAddress().getWard().getDistrict().getProvince().getId()));
                            }
                        }
                        // Split street into house number and street (assuming space-separated)
                        String street = hostel.getAddress().getStreet();
                        if (street != null && !street.isEmpty()) {
                            String[] parts = street.split(" ", 2);
                            hostelDTO.setHouseNumber(parts[0]);
                            hostelDTO.setStreet(parts.length > 1 ? parts[1] : "");
                        }
                    }
                    isEdit = true;
                } else {
                    model.addAttribute("errorMessage", "Không tìm thấy khu trọ với ID: " + id);
                }
            }

            model.addAttribute("hostel", hostelDTO);
            model.addAttribute("isEdit", isEdit);
            return "host/themkhutro";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("isEdit", false);
            return "host/themkhutro";
        }
    }

    @PostMapping("/chu-tro/luu-khu-tro")
    @ResponseBody
    public ResponseEntity<Map<String, String>> luuKhuTro(
            @RequestBody HostelDTO hostelDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, String> response = new HashMap<>();
        try {
            // Validate required fields
            if (hostelDTO.getName() == null || hostelDTO.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Tên khu trọ không được để trống");
            }
            if (hostelDTO.getRoomNumber() == null || hostelDTO.getRoomNumber() < 1) {
                throw new IllegalArgumentException("Số phòng phải lớn hơn 0");
            }
            if (hostelDTO.getProvince() == null || hostelDTO.getProvince().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn tỉnh/thành phố");
            }
            if (hostelDTO.getDistrict() == null || hostelDTO.getDistrict().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn quận/huyện");
            }
            if (hostelDTO.getWard() == null || hostelDTO.getWard().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn phường/xã");
            }

            // Set owner ID
            hostelDTO.setOwnerId(userDetails.getUser().getUserId());

            // Save or update
            if (hostelDTO.getHostelId() != null && hostelDTO.getHostelId() > 0) {
                hostelService.updateHostel(hostelDTO);
                response.put("successMessage", "Cập nhật khu trọ thành công!");
            } else {
                hostelService.createHostel(hostelDTO);
                response.put("successMessage", "Thêm khu trọ mới thành công!");
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/chu-tro/xoa-khu-tro")
    public String xoaKhuTro(@RequestParam Integer hostelId, RedirectAttributes redirectAttributes) {
        try {
            if (hostelId == null || hostelId <= 0) {
                throw new IllegalArgumentException("ID khu trọ không hợp lệ");
            }
            hostelService.deleteHostel(hostelId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa khu trọ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa khu trọ: " + e.getMessage());
        }
        return "redirect:/chu-tro/thong-tin-tro";
    }
}