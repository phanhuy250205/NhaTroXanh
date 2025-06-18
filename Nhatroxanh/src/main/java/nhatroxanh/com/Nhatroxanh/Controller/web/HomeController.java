package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import nhatroxanh.com.Nhatroxanh.Model.enity.ElectricWaterReading;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Repository.ElectricWaterReadingRepository;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;

@Controller
public class HomeController {
    @Autowired
    private RoomsService roomsService;
    @Autowired
    private RoomsRepository roomRepository;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UtilityRepository utilityRepository;

    @Autowired
    private ElectricWaterReadingRepository electricWaterReadingRepository;

    @GetMapping("/")
    public String home(Model model) {
        List<Rooms> rooms = roomsService.findAllRooms();
        model.addAttribute("rooms", rooms);
        return "index";
    }
   

    @GetMapping("/demo")
    public String demo() {
        return "guest/demo";
    }

    @Transactional
    @GetMapping("/chi-tiet/{room_id}")
    public String getRoomDetail(@PathVariable("room_id") @Min(1) Integer roomId, Model model) {

        Optional<Rooms> roomOptional = roomRepository.findByIdWithDetails(roomId);
        if (roomOptional.isEmpty()) {
            return "error/404";
        }
        Rooms room = roomOptional.get();
        model.addAttribute("room", room);

        Hostel hostel = room.getHostel();
        model.addAttribute("hostel", hostel);

        Users owner = hostel != null ? hostel.getOwner() : null;
        model.addAttribute("owner", owner);

        Set<Utility> utilities = room.getUtilities();
        System.out.println("Utilities for Room ID " + roomId + ": " + utilities);
        if (utilities != null && !utilities.isEmpty()) {
            utilities.forEach(utility -> System.out.println("Utility Name: " + utility.getName()));
        } else {
            System.out.println("No utilities found for Room ID " + roomId);
        }
        model.addAttribute("utilities", utilities != null ? utilities : new HashSet<>());

        Optional<ElectricWaterReading> readingOptional = electricWaterReadingRepository.findById(roomId);
        if (readingOptional.isPresent()) {
            model.addAttribute("electricWaterReading", readingOptional.get());
        }

        model.addAttribute("ratingScore", 4.8);
        model.addAttribute("reviewCount", 24);

        // Mock image list (replace with actual image data)
        List<String> images = List.of("/images/cards/anh1.jpg", "/images/cards/anh1.jpg", "/images/cards/anh1.jpg");
        model.addAttribute("images", images);

        Integer currentHostelId = room.getHostel() != null ? room.getHostel().getHostelId() : null;
        List<Rooms> similarRooms = new ArrayList<>();
        if (currentHostelId != null) {
            similarRooms = roomRepository.findAllWithDetails().stream()
                    .filter(r -> r.getHostel() != null && r.getHostel().getHostelId() != null)
                    .filter(r -> !r.getRoom_id().equals(roomId))
                    .filter(r -> r.getHostel().getHostelId().equals(currentHostelId))
                    .limit(4)
                    .collect(Collectors.toList());
        }

        if (similarRooms.isEmpty() && room.getCategory() != null) {
            System.out.println("Falling back to category filter...");
            similarRooms = roomRepository.findAllWithDetails().stream()
                    .filter(r -> !r.getRoom_id().equals(roomId) && r.getCategory() != null
                            && r.getCategory().equals(room.getCategory()))
                    .limit(4)
                    .collect(Collectors.toList());
        }

        if (similarRooms.isEmpty()) {
            System.out.println("Falling back to broader filter...");
            similarRooms = roomRepository.findAllWithDetails().stream()
                    .filter(r -> !r.getRoom_id().equals(roomId))
                    .limit(4)
                    .collect(Collectors.toList());
        }

        model.addAttribute("similarRooms", similarRooms);
        System.out.println("Final Similar Rooms: " + similarRooms.size());

        return "/guest/chi-tiet";
    }

    @GetMapping("/phong-tro")
    public String danhmuc() {
        return "guest/phong-tro";
    }

   
    
    
}
