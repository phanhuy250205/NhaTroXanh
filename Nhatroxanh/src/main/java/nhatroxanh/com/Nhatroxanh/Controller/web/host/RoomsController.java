package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Image;
import nhatroxanh.com.Nhatroxanh.Model.entity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.Utility;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ImageRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.FileUploadService;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Controller
public class RoomsController {

    private static final Logger log = Logger.getLogger(RoomsController.class.getName());

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

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/chu-tro/quan-ly-tro")
    public String showRoomList(@RequestParam(required = false) Integer hostelId,
            @RequestParam(required = false) String status,
            Model model) {
        Integer ownerId = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            ownerId = ((CustomUserDetails) principal).getUserId();
        }

        if (ownerId == null) {
            log.warning("Unauthorized access: No user ID found");
            model.addAttribute("errorMessage", "Bạn cần đăng nhập để xem danh sách phòng trọ.");
            model.addAttribute("rooms", List.of());
            model.addAttribute("hostels", List.of());
            model.addAttribute("hostelId", null);
            model.addAttribute("utilities", List.of());
            return "host/phongtro";
        }

        List<Hostel> hostels = hostelService.getHostelsByOwnerId(ownerId);
        model.addAttribute("hostels", hostels);

        if (hostelId == null && !hostels.isEmpty()) {
            hostelId = hostels.get(0).getHostelId();
        }

        List<Rooms> rooms = new ArrayList<>();
        if (hostelId != null) {
            List<ContractDto.Room> roomDtos = roomsService.getRoomsByHostelId(hostelId);
            rooms = roomDtos.stream()
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
                    .filter(room -> status == null || status.isEmpty()
                            || room.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        } else {
            model.addAttribute("errorMessage", "Bạn chưa có khu trọ nào. Vui lòng tạo khu trọ trước.");
        }

        long totalRooms = roomsRepository.countRoomsByOwnerId(ownerId);
        long availableRooms = roomsRepository.countVacantRoomsByOwnerId(ownerId);
        long occupiedRooms = roomsRepository.countRentedRoomsByOwnerId(ownerId);
        long maintenanceRooms = roomsRepository.countByStatus(RoomStatus.repair);

        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("occupiedRooms", occupiedRooms);
        model.addAttribute("maintenanceRooms", maintenanceRooms);

        List<Utility> allUtilities = utilityRepository.findAll();
        model.addAttribute("rooms", rooms);
        model.addAttribute("hostelId", hostelId);
        model.addAttribute("utilities", allUtilities);
        model.addAttribute("selectedStatus", status);

        return "host/phongtro";
    }

    @GetMapping("/chu-tro/cap-nhat-phong/{roomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomById(@PathVariable Integer roomId) {
        log.info("Fetching room with ID: " + roomId);
        Optional<Rooms> roomOpt = roomsService.findById(roomId);
        if (roomOpt.isEmpty()) {
            log.warning("Room not found: " + roomId);
            return ResponseEntity.notFound().build();
        }

        Rooms room = roomOpt.get();
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("roomId", room.getRoomId());
        roomData.put("namerooms", room.getNamerooms());
        roomData.put("price", room.getPrice());
        roomData.put("acreage", room.getAcreage());
        roomData.put("status", room.getStatus().name());
        roomData.put("maxTenants", room.getMax_tenants());
        roomData.put("hostelId", room.getHostel().getHostelId());
        roomData.put("description", room.getDescription());

        Set<Utility> utilities = roomsService.getUtilitiesByRoomId(roomId);
        List<String> amenityNames = utilities.stream()
                .map(Utility::getName)
                .collect(Collectors.toList());
        roomData.put("amenities", amenityNames);

        List<Image> images = imageRepository.findByRoom(room);
        List<String> imagePaths = images.stream()
                .map(Image::getUrl)
                .collect(Collectors.toList());
        roomData.put("images", imagePaths);

        log.info("Returning room data for ID: " + roomId);
        return ResponseEntity.ok(roomData);
    }

    @PostMapping("/chu-tro/them-phong")
    public String themPhongMoi(
            @RequestParam("namerooms") String namerooms,
            @RequestParam("price") Float price,
            @RequestParam("acreage") Float acreage,
            @RequestParam("status") String status,
            @RequestParam("maxTenants") Integer maxTenants,
            @RequestParam("hostelId") Integer hostelId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "amenities", required = false) List<String> amenities,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            RedirectAttributes redirectAttributes) {
        log.info("Processing new room creation for hostelId: " + hostelId);
        Optional<Hostel> hostelOpt = hostelRepository.findById(hostelId);
        if (hostelOpt.isEmpty()) {
            log.warning("Hostel not found: " + hostelId);
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khu trọ.");
            return "redirect:/chu-tro/quan-ly-tro";
        }

        if (namerooms == null || namerooms.trim().isEmpty()) {
            log.warning("Room name is empty");
            redirectAttributes.addFlashAttribute("errorMessage", "Tên phòng không được để trống.");
            return "redirect:/chu-tro/quan-ly-tro";
        }

        Rooms room = new Rooms();
        room.setNamerooms(namerooms);
        room.setPrice(price);
        room.setAcreage(acreage);
        try {
            if (!Arrays.asList("unactive", "active", "repair").contains(status)) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
            room.setStatus(RoomStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            log.warning("Invalid status: " + status + ", defaulting to unactive");
            room.setStatus(RoomStatus.unactive);
        }
        room.setMax_tenants(maxTenants);
        room.setHostel(hostelOpt.get());
        room.setDescription(description);

        if (amenities != null && !amenities.isEmpty()) {
            Set<Utility> utilities = amenities.stream()
                    .map(name -> utilityRepository.findByName(name).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            room.setUtilities(utilities);
        }

        Rooms savedRoom = roomsService.save(room);
        log.info("Saved room with ID: " + savedRoom.getRoomId());

        if (images != null && !images.isEmpty()) {
            try {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        if (!image.getContentType().startsWith("image/")) {
                            log.warning("Invalid file type: " + image.getContentType());
                            redirectAttributes.addFlashAttribute("error", "Chỉ chấp nhận file ảnh!");
                            return "redirect:/chu-tro/quan-ly-tro?hostelId=" + hostelId;
                        }
                        String imagePath = fileUploadService.uploadFile(image, "");
                        Image imageEntity = Image.builder()
                                .url(imagePath)
                                .room(savedRoom)
                                .type(Image.ImageType.OTHER)
                                .build();
                        imageRepository.save(imageEntity);
                        log.info("Saved image: " + imagePath + " for room: " + savedRoom.getRoomId());
                    }
                }
            } catch (IOException e) {
                log.severe("Error uploading images: " + e.getMessage());
                redirectAttributes.addFlashAttribute("error", "Lỗi khi tải ảnh lên: " + e.getMessage());
                return "redirect:/chu-tro/quan-ly-tro?hostelId=" + hostelId;
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Thêm phòng thành công!");
        return "redirect:/chu-tro/quan-ly-tro?hostelId=" + hostelId;
    }

    @PostMapping("/chu-tro/cap-nhat-phong")
    public String updateRoom(
            @RequestParam("roomId") Integer roomId,
            @RequestParam("namerooms") String namerooms,
            @RequestParam("price") Float price,
            @RequestParam("acreage") Float acreage,
            @RequestParam("status") String status,
            @RequestParam("maxTenants") Integer maxTenants,
            @RequestParam("hostelId") Integer hostelId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "amenities", required = false) List<String> amenities,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "existingImages", required = false) String existingImages,
            RedirectAttributes redirectAttributes) {
        log.info("Updating room with ID: " + roomId);
        Optional<Rooms> roomOpt = roomsService.findById(roomId);
        if (roomOpt.isEmpty()) {
            log.warning("Room not found: " + roomId);
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phòng trọ.");
            return "redirect:/chu-tro/quan-ly-tro?hostelId=" + hostelId;
        }

        if (namerooms == null || namerooms.trim().isEmpty()) {
            log.warning("Room name is empty for roomId: " + roomId);
            redirectAttributes.addFlashAttribute("errorMessage", "Tên phòng không được để trống.");
            return "redirect:/chu-tro/quan-ly-tro?hostelId=" + hostelId;
        }

        Rooms room = roomOpt.get();
        room.setNamerooms(namerooms);
        room.setPrice(price);
        room.setAcreage(acreage);
        try {
            if (!Arrays.asList("unactive", "active", "repair").contains(status)) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
            room.setStatus(RoomStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            log.warning("Invalid status: " + status + ", defaulting to unactive");
            room.setStatus(RoomStatus.unactive);
        }
        room.setMax_tenants(maxTenants);
        room.setDescription(description);

        if (amenities != null && !amenities.isEmpty()) {
            Set<Utility> utilities = amenities.stream()
                    .map(name -> utilityRepository.findByName(name).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            room.setUtilities(utilities);
        }

        // Handle existing images
        List<Image> currentImages = imageRepository.findByRoom(room);
        List<String> imagesToKeep = existingImages != null && !existingImages.isEmpty() 
            ? Arrays.asList(existingImages.split(","))
            : new ArrayList<>();
        
        // Delete images that are not in the keep list
        for (Image currentImage : currentImages) {
            if (!imagesToKeep.contains(currentImage.getUrl())) {
                fileUploadService.deleteFile(currentImage.getUrl());
                imageRepository.delete(currentImage);
                log.info("Deleted image: " + currentImage.getUrl());
            }
        }

        // Handle new image uploads
        if (images != null && !images.isEmpty()) {
            try {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        if (!image.getContentType().startsWith("image/")) {
                            log.warning("Invalid file type: " + image.getContentType());
                            redirectAttributes.addFlashAttribute("error", "Chỉ chấp nhận file ảnh!");
                            return "redirect:/chu-tro/quan-ly-tro?hostelId=" + hostelId;
                        }
                        String imagePath = fileUploadService.uploadFile(image, "");
                        Image imageEntity = Image.builder()
                                .url(imagePath)
                                .room(room)
                                .type(Image.ImageType.OTHER)
                                .build();
                        imageRepository.save(imageEntity);
                        log.info("Saved image: " + imagePath + " for room: " + room.getRoomId());
                    }
                }
            } catch (IOException e) {
                log.severe("Error uploading images: " + e.getMessage());
                redirectAttributes.addFlashAttribute("error", "Lỗi khi tải ảnh lên: " + e.getMessage());
                return "redirect:/chu-tro/quan-ly-tro?hostelId=" + hostelId;
            }
        }

        roomsService.save(room);
        log.info("Updated room with ID: " + room.getRoomId());
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phòng trọ thành công!");
        return "redirect:/chu-tro/quan-ly-tro?hostelId=" + hostelId;
    }

    @PostMapping("/chu-tro/xoa-phong/{roomId}")
    public String deleteRoom(@PathVariable Integer roomId, RedirectAttributes redirectAttributes,
            @RequestParam(required = false) Integer hostelId) {
        log.info("Deleting room with ID: " + roomId);
        Optional<Rooms> roomOpt = roomsRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            log.warning("Room not found: " + roomId);
            redirectAttributes.addFlashAttribute("errorMessage", "Phòng trọ không tồn tại.");
            return "redirect:/chu-tro/quan-ly-tro?hostelId=" + (hostelId != null ? hostelId : "");
        }

        Rooms room = roomOpt.get();
        Integer currentHostelId = room.getHostel().getHostelId();

        List<Image> images = imageRepository.findByRoom(room);
        if (!images.isEmpty()) {
            for (Image image : images) {
                fileUploadService.deleteFile(image.getUrl());
                imageRepository.delete(image);
                log.info("Deleted image: " + image.getUrl());
            }
        }

        roomsRepository.delete(room);
        log.info("Deleted room with ID: " + roomId);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa phòng trọ thành công!");
        return "redirect:/chu-tro/quan-ly-tro?hostelId=" + currentHostelId;
    }

    @PostMapping("/chu-tro/cap-nhat-trang-thai-phong")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateRoomStatus(@RequestParam Integer roomId,
            @RequestParam String status,
            @RequestParam(required = false) Integer hostelId) {
        log.info("Updating status for room ID: " + roomId + " to " + status);
        Integer ownerId = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            ownerId = ((CustomUserDetails) principal).getUserId();
        }
        if (ownerId == null) {
            log.warning("Unauthorized access: No user ID found");
            return ResponseEntity.status(401)
                    .body(Collections.singletonMap("error", "Bạn cần đăng nhập để thực hiện thao tác này."));
        }

        Optional<Rooms> roomOpt = roomsService.findById(roomId);
        if (roomOpt.isEmpty()) {
            log.warning("Room not found: " + roomId);
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Phòng trọ không tồn tại."));
        }

        Rooms room = roomOpt.get();
        if (!room.getHostel().getOwner().getUserId().equals(ownerId)) {
            log.warning("Forbidden: User " + ownerId + " does not own room " + roomId);
            return ResponseEntity.status(403)
                    .body(Collections.singletonMap("error", "Bạn không có quyền cập nhật phòng này."));
        }

        try {
            if (!Arrays.asList("unactive", "active", "repair").contains(status)) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
            RoomStatus newStatus = RoomStatus.valueOf(status);
            room.setStatus(newStatus);
            roomsService.save(room);
            log.info("Updated status for room ID: " + roomId + " to " + newStatus);
        } catch (IllegalArgumentException e) {
            log.warning("Invalid status: " + status);
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Trạng thái không hợp lệ."));
        }

        long totalRooms = roomsRepository.countRoomsByOwnerId(ownerId);
        long availableRooms = roomsRepository.countVacantRoomsByOwnerId(ownerId);
        long occupiedRooms = roomsRepository.countRentedRoomsByOwnerId(ownerId);
        long maintenanceRooms = roomsRepository.countByStatus(RoomStatus.repair);

        Map<String, Object> response = new HashMap<>();
        response.put("success", "Cập nhật trạng thái phòng thành công!");
        response.put("totalRooms", totalRooms);
        response.put("availableRooms", availableRooms);
        response.put("occupiedRooms", occupiedRooms);
        response.put("maintenanceRooms", maintenanceRooms);
        response.put("hostelId", hostelId != null ? hostelId : room.getHostel().getHostelId());

        return ResponseEntity.ok(response);
    }
}