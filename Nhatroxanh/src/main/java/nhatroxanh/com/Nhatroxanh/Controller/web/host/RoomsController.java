package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import nhatroxanh.com.Nhatroxanh.Model.Dto.RoomCreateDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Image;
import nhatroxanh.com.Nhatroxanh.Model.enity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ImageRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class RoomsController {

    @Autowired
    private RoomsService roomsService;

    @Autowired
    private HostelService hostelService;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private UtilityRepository utilityRepository;

    @Autowired
    private RoomsRepository roomsRepository;

    @Autowired
    private ImageRepository imageRepository;

    @GetMapping("/chu-tro/quan-ly-tro")
    public String showRoomList(@RequestParam(required = false) Integer hostelId, Model model) {
        Integer ownerId = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            ownerId = ((CustomUserDetails) principal).getUserId();
        }

        if (ownerId == null) {
            model.addAttribute("error", "Bạn cần đăng nhập để xem danh sách phòng trọ.");
            model.addAttribute("rooms", List.of());
            model.addAttribute("hostels", List.of());
            model.addAttribute("hostelId", null);
            model.addAttribute("utilities", List.of()); // Thêm danh sách tiện ích rỗng
            return "host/phongtro";
        }

        List<Hostel> hostels = hostelService.getHostelsByOwnerId(ownerId);
        model.addAttribute("hostels", hostels);

        if (hostelId == null && !hostels.isEmpty()) {
            hostelId = hostels.get(0).getHostelId();
        }

        List<Rooms> rooms;
        if (hostelId != null) {
            rooms = roomsService.getRoomsByHostelId(hostelId).stream()
                    .map(dto -> {
                        Rooms room = new Rooms();
                        room.setRoomId(dto.getRoomId());
                        room.setNamerooms(dto.getRoomName());
                        room.setPrice(dto.getPrice());
                        room.setStatus(RoomStatus.fromString(dto.getStatus()));
                        room.setAcreage(dto.getArea());
                        room.setMax_tenants(dto.getMaxTenants());
                        return room;
                    })
                    .collect(Collectors.toList());
        } else {
            rooms = List.of();
            model.addAttribute("error", "Bạn chưa có khu trọ nào. Vui lòng tạo khu trọ trước.");
        }

        // Lấy tất cả tiện ích để hiển thị trong form
        List<Utility> allUtilities = utilityRepository.findAll();
        model.addAttribute("rooms", rooms);
        model.addAttribute("hostelId", hostelId);
        model.addAttribute("utilities", allUtilities); // Truyền danh sách tiện ích

        return "host/phongtro";
    }

   
    @GetMapping("/chu-tro/cap-nhat-phong/{roomId}")
    @ResponseBody
    public ResponseEntity<RoomCreateDTO> getRoomById(@PathVariable Integer roomId) {
        Optional<Rooms> roomOpt = roomsService.findById(roomId);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Rooms room = roomOpt.get();
        RoomCreateDTO roomDTO = new RoomCreateDTO();
        roomDTO.setRoomId(room.getRoomId());
        roomDTO.setNamerooms(room.getNamerooms());
        roomDTO.setPrice(room.getPrice());
        roomDTO.setAcreage(room.getAcreage());
        roomDTO.setStatus(room.getStatus().name());
        roomDTO.setMaxTenants(room.getMax_tenants());
        roomDTO.setHostelId(room.getHostel().getHostelId());
        roomDTO.setDescription(room.getDescription());

        // Lấy danh sách tiện ích của phòng
        Set<Utility> utilities = roomsService.getUtilitiesByRoomId(roomId);
        List<String> amenityNames = utilities.stream()
                .map(Utility::getName)
                .collect(Collectors.toList());
        roomDTO.setAmenities(amenityNames);

        return ResponseEntity.ok(roomDTO);
    }
    @PostMapping("/chu-tro/them-phong")
    public String themPhongMoi(@ModelAttribute RoomCreateDTO roomDTO, RedirectAttributes redirectAttributes) {
        Optional<Hostel> hostelOpt = hostelRepository.findById(roomDTO.getHostelId());
        if (hostelOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy khu trọ.");
            return "redirect:/chu-tro/phong-tro";
        }

        Rooms room = new Rooms();
        room.setNamerooms(roomDTO.getNamerooms());
        room.setPrice(roomDTO.getPrice());
        room.setAcreage(roomDTO.getAcreage());
        room.setStatus(RoomStatus.valueOf(roomDTO.getStatus()));
        room.setMax_tenants(roomDTO.getMaxTenants());
        room.setHostel(hostelOpt.get());

        // Xử lý tiện ích
        if (roomDTO.getAmenities() != null && !roomDTO.getAmenities().isEmpty()) {
            Set<Utility> utilities = roomDTO.getAmenities().stream()
                    .map(name -> utilityRepository.findByName(name).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            room.setUtilities(utilities);
        }

        roomsService.save(room);
        redirectAttributes.addFlashAttribute("success", "Thêm phòng thành công!");
        return "redirect:/chu-tro/quan-ly-tro?hostelId=" + roomDTO.getHostelId();
    }

    @PostMapping("/chu-tro/cap-nhat-phong")
    public String updateRoom(@ModelAttribute RoomCreateDTO roomDTO, RedirectAttributes redirectAttributes) {
        Optional<Rooms> roomOpt = roomsService.findById(roomDTO.getRoomId());
        if (roomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy phòng trọ.");
            return "redirect:/chu-tro/quan-ly-tro?hostelId=" + roomDTO.getHostelId();
        }

        Rooms room = roomOpt.get();
        room.setNamerooms(roomDTO.getNamerooms());
        room.setPrice(roomDTO.getPrice());
        room.setAcreage(roomDTO.getAcreage());
        room.setStatus(RoomStatus.valueOf(roomDTO.getStatus()));
        room.setMax_tenants(roomDTO.getMaxTenants());
        room.setDescription(roomDTO.getDescription());

        // Xử lý tiện ích
        if (roomDTO.getAmenities() != null && !roomDTO.getAmenities().isEmpty()) {
            Set<Utility> utilities = roomDTO.getAmenities().stream()
                    .map(name -> utilityRepository.findByName(name).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            room.setUtilities(utilities);
        }

        roomsService.save(room);
        redirectAttributes.addFlashAttribute("success", "Cập nhật phòng trọ thành công!");
        return "redirect:/chu-tro/quan-ly-tro?hostelId=" + roomDTO.getHostelId();
    }

    @PostMapping("/chu-tro/xoa-phong/{roomId}")
    public String deleteRoom(@PathVariable Integer roomId, RedirectAttributes redirectAttributes, @RequestParam(required = false) Integer hostelId) {
        Optional<Rooms> roomOpt = roomsRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Phòng trọ không tồn tại.");
            return "redirect:/chu-tro/quan-ly-tro?hostelId=" + (hostelId != null ? hostelId : "");
        }

        Rooms room = roomOpt.get();
        Integer currentHostelId = room.getHostel().getHostelId();

        // Xóa các ảnh liên quan
        List<Image> images = imageRepository.findByRoom(room);
        if (!images.isEmpty()) {
            imageRepository.deleteAll(images);
        }

        // Xóa phòng
        roomsRepository.delete(room);
        redirectAttributes.addFlashAttribute("success", "Xóa phòng trọ thành công!");
        return "redirect:/chu-tro/quan-ly-tro?hostelId=" + currentHostelId;
    }

}
