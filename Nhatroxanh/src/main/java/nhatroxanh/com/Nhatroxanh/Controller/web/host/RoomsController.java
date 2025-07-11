package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class RoomsController {

    @Autowired
    private RoomsService roomsService;

    @Autowired
    private HostelService hostelService;

    @GetMapping("/chu-tro/quan-ly-tro")
    public String showRoomList(@RequestParam(required = false) Integer hostelId, Model model) {
        // Get ownerId from authenticated user
        Integer ownerId = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            ownerId = ((CustomUserDetails) principal).getUserId();
        }

        if (ownerId == null) {
            // Handle case where user is not authenticated
            model.addAttribute("error", "Bạn cần đăng nhập để xem danh sách phòng trọ.");
            model.addAttribute("rooms", List.of());
            model.addAttribute("hostels", List.of());
            model.addAttribute("hostelId", null);
            return "host/phongtro";
        }

        // Get list of hostels for the dropdown
        List<Hostel> hostels = hostelService.getHostelsByOwnerId(ownerId);
        model.addAttribute("hostels", hostels);

        // If hostelId is not provided, use the first hostelId from the owner's hostels
        if (hostelId == null && !hostels.isEmpty()) {
            hostelId = hostels.get(0).getHostelId();
        }

        // Fetch rooms by hostelId
        List<Rooms> rooms;
        if (hostelId != null) {
            rooms = roomsService.getRoomsByHostelId(hostelId)
                    .stream()
                    .map(dto -> {
                        Rooms room = new Rooms();
                        room.setRoomId(dto.getRoomId());
                        room.setNamerooms(dto.getRoomName());
                        room.setPrice(dto.getPrice());
                        try {
                            room.setStatus(dto.getStatus() != null ? RoomStatus.valueOf(dto.getStatus()) : RoomStatus.unactive);
                        } catch (IllegalArgumentException e) {
                            room.setStatus(RoomStatus.unactive);
                        }
                        room.setAcreage(dto.getArea());
                        room.setMax_tenants(dto.getMaxTenants());
                        return room;
                    })
                    .collect(Collectors.toList());
        } else {
            rooms = List.of(); // Empty list if no hostel is available
            model.addAttribute("error", "Bạn chưa có khu trọ nào. Vui lòng tạo khu trọ trước.");
        }

        // Add rooms and selected hostelId to the model
        model.addAttribute("rooms", rooms);
        model.addAttribute("hostelId", hostelId);

        return "host/phongtro";
    }
}