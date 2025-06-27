package nhatroxanh.com.Nhatroxanh.Controller.web;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.Ward;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.TemporaryResidenceRepository;
import nhatroxanh.com.Nhatroxanh.Repository.WardRepository;
import nhatroxanh.com.Nhatroxanh.Service.AddressService;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/chu-tro")
public class RoomManagementController {

    @Autowired
    private HostelService hostelService;

    @Autowired
    private HostelRepository hostelRepository; 

    @Autowired
    private RoomsService roomsService;

    @Autowired
    private RoomsRepository roomsRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private AddressService addressService;

    @Autowired
    private TemporaryResidenceRepository temporaryResidenceRepository;

    @GetMapping("/search-hostels")
    @ResponseBody
    public List<Hostel> searchHostels(@RequestParam("name") String name) {
        return hostelService.findHostelsByName(name);
    }

    @GetMapping("/thong-tin-tro")
    public String thongTinTro(@RequestParam(value = "name", required = false) String name, Model model) {
        List<Hostel> hostels;
        if (name != null && !name.isEmpty()) {
            hostels = hostelService.findHostelsByName(name);
        } else {
            hostels = hostelService.findAllHostels();
        }
        model.addAttribute("hostels", hostels);
        model.addAttribute("searchName", name); 
        return "host/thongtintro";
    }

    @GetMapping("/them-khu-tro")
    public String themKhuTro(Model model, @RequestParam(value = "id", required = false) Integer id) {
        Hostel hostel;
        if (id != null) {
            hostel = hostelService.findHostelById(id).orElse(new Hostel());
        } else {
            hostel = new Hostel();
            hostel.setStatus(true); // Mặc định trạng thái là true
            hostel.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
        }
        if (hostel.getAddress() == null) {
            hostel.setAddress(new Address());
        }
        model.addAttribute("hostel", hostel);
        return "host/themkhutro";
    }

   @PostMapping("/save-khu-tro")
@Transactional
public String saveKhuTro(@ModelAttribute Hostel hostel, 
                        @RequestParam(value = "wardHost", required = false) String wardCode,
                        @RequestParam(value = "streetHost", required = false) String street,
                        @RequestParam(value = "houseNumberHost", required = false) String houseNumber,
                        Model model) {
    Address address = hostel.getAddress();
    if (address == null) {
        address = new Address();
        hostel.setAddress(address);
    }

    address.setStreet((houseNumber != null ? houseNumber + ", " : "") + (street != null ? street : ""));

    if (wardCode != null && !wardCode.isEmpty()) {
        Ward ward = wardRepository.findByCode(wardCode)
                .orElseThrow(() -> new RuntimeException("Ward not found for code: " + wardCode));
        address.setWard(ward);
    } else {
        model.addAttribute("error", "Vui lòng chọn phường/xã.");
        model.addAttribute("hostel", hostel);
        return "host/themkhutro";
    }

    if (hostel.getHostelId() == null) {
        hostel.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
        if (hostel.getStatus() == null) hostel.setStatus(true);
    }

    try {
        // Kiểm tra và xử lý ràng buộc trước khi lưu
        if (hostel.getHostelId() != null) {
            Hostel existingHostel = hostelService.findHostelById(hostel.getHostelId())
                    .orElseThrow(() -> new RuntimeException("Hostel not found"));
            if (existingHostel.getAddress() != null) {
                // Thêm logic kiểm tra và xóa/cập nhật temporary_residence nếu cần
                // Ví dụ: Cần một repository cho temporary_residence
                // temporaryResidenceRepository.deleteByAddressId(existingHostel.getAddress().getId());
            }
        }
        hostelService.saveHostel(hostel);
        System.out.println("Hostel saved successfully with ID: " + hostel.getHostelId());
    } catch (Exception e) {
        e.printStackTrace();
        model.addAttribute("error", "Lỗi khi lưu khu trọ: " + e.getMessage());
        model.addAttribute("hostel", hostel);
        return "host/themkhutro";
    }
    return "redirect:/chu-tro/thong-tin-tro";
}

@GetMapping("/edit-khu-tro")
public String editKhuTro(@RequestParam Integer id, Model model) {
    Hostel hostel = hostelService.findHostelById(id)
            .orElseThrow(() -> new RuntimeException("Hostel not found with id: " + id));
    if (hostel.getAddress() == null) {
        hostel.setAddress(new Address());
    }
    model.addAttribute("hostel", hostel);
    return "host/themkhutro";
}

    @GetMapping("/delete-khu-tro")
    @Transactional
    public String deleteKhuTro(@RequestParam Integer id) {
        hostelService.deleteHostelById(id);
        return "redirect:/chu-tro/thong-tin-tro";
    }


    private String getProvinceName(String code) {
        return wardRepository.findByCode(code.split("-")[0])
                .map(w -> w.getDistrict().getProvince().getName())
                .orElse("Unknown");
    }

    private String getDistrictName(String code) {
        return wardRepository.findByCode(code)
                .map(w -> w.getDistrict().getName())
                .orElse("Unknown");
    }

    private String getWardName(String code) {
        return wardRepository.findByCode(code)
                .map(Ward::getName)
                .orElse("Unknown");
    }
   @GetMapping("/quan-ly-tro")
public String manageRooms(Model model, Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated()) {
        String email = authentication.getName();
        System.out.println("User email: " + email);
        List<Hostel> hostels = hostelRepository.findAllByOwnerEmail(email);
        if (hostels != null && !hostels.isEmpty()) {
            List<Rooms> allRooms = new ArrayList<>();
            for (Hostel hostel : hostels) {
                System.out.println("Checking hostel: " + hostel.getHostelId());
                List<Rooms> rooms = roomsRepository.findByHostel_HostelId(hostel.getHostelId());
                allRooms.addAll(rooms);
                System.out.println("Rooms found for hostel " + hostel.getHostelId() + ": " + rooms.size());
            }
            model.addAttribute("rooms", allRooms);
            System.out.println("Total rooms loaded: " + allRooms.size());
        } else {
            model.addAttribute("rooms", new ArrayList<>());
            model.addAttribute("error", "Không tìm thấy khu trọ của người dùng cho email: " + email);
            System.out.println("No hostels found for email: " + email);
        }
    } else {
        model.addAttribute("rooms", new ArrayList<>());
        model.addAttribute("error", "Vui lòng đăng nhập để quản lý phòng.");
        System.out.println("User not authenticated");
    }
    return "host/phongtro";
}
    @GetMapping("/them-phong")
    public String addRoom(Model model, Principal principal) {
        Rooms room = new Rooms();
        room.setStatus(true); // Mặc định Trống
        if (principal != null) {
            String email = principal.getName();
            List<Hostel> hostels = hostelRepository.findAllByOwnerEmail(email);
            if (!hostels.isEmpty()) {
                room.setHostel(hostels.get(0)); // Lấy hostel đầu tiên
            } else {
                model.addAttribute("error", "Không tìm thấy khu trọ của người dùng.");
            }
        } else {
            model.addAttribute("error", "Vui lòng đăng nhập để thêm phòng.");
        }
        model.addAttribute("room", room);
        model.addAttribute("hostels", hostelRepository.findAllByOwnerEmail(principal != null ? principal.getName() : null));
        return "host/themphongtro";
    }

    @PostMapping("/save-room")
    public String saveRoom(@ModelAttribute("room") Rooms room, Model model, Principal principal) {
        try {
            if (principal != null) {
                String email = principal.getName();
                List<Hostel> hostels = hostelRepository.findAllByOwnerEmail(email);
                if (hostels.isEmpty()) {
                    model.addAttribute("error", "Không tìm thấy khu trọ của người dùng.");
                    return "host/themphongtro";
                }
                room.setHostel(hostels.get(0)); // Gán hostel
            } else {
                model.addAttribute("error", "Vui lòng đăng nhập để thêm phòng.");
                return "host/themphongtro";
            }
            roomsService.saveRoom(room); // Gọi service để lưu
            model.addAttribute("success", "Thêm phòng thành công!");
            return "redirect:/chu-tro/quan-ly-tro"; // Quay lại danh sách
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi lưu phòng: " + e.getMessage());
            return "host/themphongtro"; // Quay lại trang với thông báo lỗi
        }
    }
}

    