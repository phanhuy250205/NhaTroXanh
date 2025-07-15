package nhatroxanh.com.Nhatroxanh.Controller;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ApiResponse;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static nhatroxanh.com.Nhatroxanh.Util.AddressUtils.parseAddress;

@RestController
@RequestMapping("/api/rooms")
public class RoomsApiController {
    private static final Logger logger = LoggerFactory.getLogger(RoomsApiController.class);

    @Autowired
    private RoomsService roomsService;

    @Autowired
    private ContractService contractService;

    @GetMapping("/by-hostel/{hostelId}")
    public ResponseEntity<List<ContractDto.Room>> getRoomsByHostel(@PathVariable Long hostelId) {
        try {
            logger.info("🏢 API: Getting rooms for hostel ID: {}", hostelId);

            List<Rooms> roomEntities = roomsService.findByHostelId(Math.toIntExact(hostelId));  // Giả sử service trả entity

            // Convert và parse address
            List<ContractDto.Room> rooms = roomEntities.stream()
                    .map(room -> {
                        ContractDto.Room dto = convertRoomToDto(room);  // Gọi hàm convert có parseAddress
                        logger.info("Parsed room {} address: street={}, ward={}, district={}, province={}",
                                room.getRoomId(), dto.getStreet(), dto.getWard(), dto.getDistrict(), dto.getProvince());
                        return dto;
                    })
                    .collect(Collectors.toList());

            logger.info("🏠 API: Found {} rooms", rooms.size());
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            logger.error("❌ API Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ContractDto.Room> getRoomByUserId(@PathVariable Long userId) {
        try {
            // Log thông tin bắt đầu request
            logger.info("Attempting to find room for user ID: {}", userId);

            // Tìm phòng
            Rooms room = contractService.findRoomByTenantId(userId);

            // Log khi tìm thấy phòng
            logger.debug("Room found for user ID {}: Room ID {}, Name: {}",
                    userId, room.getRoomId(), room.getNamerooms());

            // Chuyển đổi sang DTO
            ContractDto.Room roomDto = new ContractDto.Room();
            roomDto.setRoomId(room.getRoomId());
            roomDto.setRoomName(room.getNamerooms());
            roomDto.setStatus(String.valueOf(room.getStatus()));
            roomDto.setAddress(room.getAddress());

            // Log thông tin địa chỉ
            logger.info("Room address for user ID {}: {}", userId, room.getAddress());

            return ResponseEntity.ok(roomDto);

        } catch (ResourceNotFoundException ex) {
            // Log lỗi không tìm thấy phòng
            logger.warn("No room found for user ID: {}", userId);
            return ResponseEntity.notFound().build();

        } catch (Exception ex) {
            // Log lỗi hệ thống
            logger.error("Error finding room for user ID {}: {}",
                    userId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/contract/{contractId}/edit")
    public ResponseEntity<?> getContractEditForm(@PathVariable Long contractId) {
        // Lấy phòng hiện tại của hợp đồng
        Rooms currentRoom = contractService.findContractById(Math.toIntExact(contractId))
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with ID: " + contractId))
                .getRoom();

        // Tạo map chứa thông tin phòng
        Map<String, Object> roomDetails = new HashMap<>();
        roomDetails.put("roomId", currentRoom.getRoomId());
        roomDetails.put("roomName", currentRoom.getNamerooms());
        roomDetails.put("address", currentRoom.getAddress());
        roomDetails.put("hostelId", currentRoom.getHostel().getHostelId());
        roomDetails.put("hostelName", currentRoom.getHostel().getName());

        // Thêm giá phòng
        roomDetails.put("roomPrice", currentRoom.getPrice() != null
                ? currentRoom.getPrice()
                : 2000000); // Giá mặc định nếu không có

        // Lấy danh sách các phòng khác trong cùng hostel
        List<Map<String, Object>> otherRooms = roomsService.getRoomsByHostelId(currentRoom.getHostel().getHostelId())
                .stream()
                .filter(room -> room.getRoomId() != currentRoom.getRoomId()) // Loại trừ phòng hiện tại
                .map(room -> {
                    Map<String, Object> roomMap = new HashMap<>();
                    roomMap.put("roomId", room.getRoomId());
                    roomMap.put("roomName", room.getRoomName());
                    roomMap.put("address", room.getAddress());
                    roomMap.put("status", room.getStatus());

                    // Thêm giá phòng cho các phòng khác
                    roomMap.put("roomPrice", room.getPrice() != null
                            ? room.getPrice()
                            : 2000000); // Giá mặc định nếu không có

                    return roomMap;
                })
                .collect(Collectors.toList());

        // Tổng hợp toàn bộ thông tin
        Map<String, Object> response = new HashMap<>();
        response.put("currentRoom", roomDetails);
        response.put("otherRooms", otherRooms);

        return ResponseEntity.ok(response);
    }

    private ContractDto.Room convertRoomToDto(Rooms room) {
        ContractDto.Room dto = new ContractDto.Room();
        dto.setRoomId(room.getRoomId());
        String nameRooms = room.getNamerooms();  // Lấy từ entity
        logger.info("Room {} name from DB: {}", room.getRoomId(), nameRooms);  // ✅ Logger debug
        dto.setRoomName(nameRooms != null && !nameRooms.trim().isEmpty() ? nameRooms : "Phòng " + room.getRoomId());  // ✅ Fallback nếu null/rỗng

        dto.setArea(room.getAcreage() != null ? room.getAcreage() : 20);  // Default 20 m²
        logger.info("Room {} area from DB: {}", room.getRoomId(), room.getAcreage());  // Debug
        dto.setPrice(room.getPrice());
        dto.setStatus(room.getStatus() != null ? room.getStatus().name() : "UNKNOWN");

        // Parse address (giữ nguyên)
        String address = room.getAddress() != null ? room.getAddress() : "";
        Map<String, String> parts = parseAddress(address);
        dto.setStreet(parts.getOrDefault("street", "Chưa cập nhật"));
        dto.setWard(parts.getOrDefault("ward", ""));
        dto.setDistrict(parts.getOrDefault("district", ""));
        dto.setProvince(parts.getOrDefault("province", ""));
        dto.setAddress(String.join(", ", parts.values()).trim());

        return dto;
    }


}

