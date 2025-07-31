package nhatroxanh.com.Nhatroxanh.Controller;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ApiResponse;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import nhatroxanh.com.Nhatroxanh.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static nhatroxanh.com.Nhatroxanh.Util.AddressUtils.parseAddress;

@RestController
@RequestMapping("/api/rooms")
@Slf4j
public class RoomsApiController {
    private static final Logger logger = LoggerFactory.getLogger(RoomsApiController.class);

    @Autowired
    private RoomsService roomsService;

    @Autowired
    private ContractService contractService;

    @GetMapping("/by-hostel/{hostelId}")
    public ResponseEntity<List<ContractDto.Room>> getRoomsByHostel(@PathVariable Long hostelId, @RequestParam(required = false) String status) {
        try {
            logger.info("🏢 API: Getting rooms for hostel ID: {}", hostelId);

            List<Rooms> roomEntities = roomsService.findByHostelId(Math.toIntExact(hostelId));
            List<ContractDto.Room> rooms = roomEntities.stream()
                    .map(this::convertRoomToDto)
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
            logger.info("Attempting to find room for user ID: {}", userId);

            Rooms room = contractService.findRoomByTenantId(userId);
            logger.debug("Room found for user ID {}: Room ID {}, Name: {}",
                    userId, room.getRoomId(), room.getNamerooms());

            ContractDto.Room roomDto = convertRoomToDto(room);
            logger.info("Room address for user ID {}: {}", userId, roomDto.getAddress());

            return ResponseEntity.ok(roomDto);
        } catch (ResourceNotFoundException ex) {
            logger.warn("No room found for user ID: {}", userId);
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            logger.error("Error finding room for user ID {}: {}", userId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/contract/{contractId}/edit")
    public ResponseEntity<?> getContractEditForm(@PathVariable Integer contractId) {
        try {
            // Lấy phòng hiện tại của hợp đồng
            Rooms currentRoom = contractService.findContractById(contractId)
                    .orElseThrow(() -> new ResourceNotFoundException("Contract not found with ID: " + contractId))
                    .getRoom();

            // Tạo map chứa thông tin phòng
            Map<String, Object> roomDetails = new HashMap<>();
            roomDetails.put("roomId", currentRoom.getRoomId());
            roomDetails.put("roomName", currentRoom.getNamerooms());
            roomDetails.put("hostelId", currentRoom.getHostel() != null ? currentRoom.getHostel().getHostelId() : null);
            roomDetails.put("hostelName", currentRoom.getHostel() != null ? currentRoom.getHostel().getName() : "Unknown");
            roomDetails.put("roomPrice", currentRoom.getPrice() != null ? currentRoom.getPrice() : 2000000);

            // Lấy địa chỉ từ Hostel
            String hostelAddress = currentRoom.getHostel() != null ? currentRoom.getHostel().getAddress() : null;
            if (hostelAddress != null && !hostelAddress.trim().isEmpty()) {
                Map<String, String> addressParts = parseAddress(hostelAddress);
                roomDetails.put("address", hostelAddress);
                roomDetails.put("street", addressParts.getOrDefault("street", "Chưa cập nhật"));
                roomDetails.put("ward", addressParts.getOrDefault("ward", ""));
                roomDetails.put("district", addressParts.getOrDefault("district", ""));
                roomDetails.put("province", addressParts.getOrDefault("province", ""));
            } else {
                roomDetails.put("address", "Địa chỉ khu trọ chưa cập nhật");
                roomDetails.put("street", "");
                roomDetails.put("ward", "");
                roomDetails.put("district", "");
                roomDetails.put("province", "");
                logger.warn("⚠️ No valid hostel address for room ID: {}", currentRoom.getRoomId());
            }

            // Lấy danh sách các phòng khác trong cùng hostel
            Integer hostelId = currentRoom.getHostel() != null ? currentRoom.getHostel().getHostelId() : null;
            if (hostelId == null) {
                throw new IllegalStateException("Phòng " + currentRoom.getRoomId() + " không thuộc khu trọ nào!");
            }

            List<Map<String, Object>> otherRooms = roomsService.getRoomsByHostelId(hostelId)
                    .stream()
                    .filter(room -> !room.getRoomId().equals(currentRoom.getRoomId()))
                    .map(room -> {
                        Map<String, Object> roomMap = new HashMap<>();
                        roomMap.put("roomId", room.getRoomId());
                        roomMap.put("roomName", room.getRoomName());
                        roomMap.put("status", room.getStatus());
                        roomMap.put("roomPrice", room.getPrice() != null ? room.getPrice() : 2000000);
                        roomMap.put("hostelId", room.getHostelId());
                        roomMap.put("hostelName", room.getHostelName());

                        // Lấy địa chỉ từ ContractDto.Room (đã được ánh xạ từ Hostel trong convertRoomToDto)
                        String otherHostelAddress = room.getAddress();
                        if (otherHostelAddress != null && !otherHostelAddress.trim().isEmpty()) {
                            Map<String, String> addressParts = parseAddress(otherHostelAddress);
                            roomMap.put("address", otherHostelAddress);
                            roomMap.put("street", addressParts.getOrDefault("street", "Chưa cập nhật"));
                            roomMap.put("ward", addressParts.getOrDefault("ward", ""));
                            roomMap.put("district", addressParts.getOrDefault("district", ""));
                            roomMap.put("province", addressParts.getOrDefault("province", ""));
                        } else {
                            roomMap.put("address", "Địa chỉ khu trọ chưa cập nhật");
                            roomMap.put("street", "");
                            roomMap.put("ward", "");
                            roomMap.put("district", "");
                            roomMap.put("province", "");
                            logger.warn("⚠️ No valid hostel address for other room ID: {}", room.getRoomId());
                        }

                        return roomMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("currentRoom", roomDetails);
            response.put("otherRooms", otherRooms);

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            logger.error("Contract not found: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            logger.error("Invalid room data: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Error processing contract edit form: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error"));
        }
    }

    private ContractDto.Room convertRoomToDto(Rooms room) {
        ContractDto.Room dto = new ContractDto.Room();
        dto.setRoomId(room.getRoomId());
        String nameRooms = room.getNamerooms();
        logger.info("Room {} name from DB: {}", room.getRoomId(), nameRooms);
        dto.setRoomName(nameRooms != null && !nameRooms.trim().isEmpty() ? nameRooms : "Phòng " + room.getRoomId());
        dto.setArea(room.getAcreage() != null ? room.getAcreage() : 20);
        logger.info("Room {} area from DB: {}", room.getRoomId(), room.getAcreage());
        dto.setPrice(room.getPrice());
        dto.setStatus(room.getStatus() != null ? room.getStatus().name() : "UNKNOWN");
        dto.setHostelId(room.getHostel() != null ? room.getHostel().getHostelId() : null);
        dto.setHostelName(room.getHostel() != null ? room.getHostel().getName() : "Unknown");

        // Lấy địa chỉ từ Hostel
        String address = room.getHostel() != null ? room.getHostel().getAddress() : null;
        if (address != null && !address.trim().isEmpty()) {
            Map<String, String> parts = parseAddress(address);
            dto.setStreet(parts.getOrDefault("street", "Chưa cập nhật"));
            dto.setWard(parts.getOrDefault("ward", ""));
            dto.setDistrict(parts.getOrDefault("district", ""));
            dto.setProvince(parts.getOrDefault("province", ""));
            dto.setAddress(address);
        } else {
            dto.setStreet("");
            dto.setWard("");
            dto.setDistrict("");
            dto.setProvince("");
            dto.setAddress("Địa chỉ khu trọ chưa cập nhật");
            logger.warn("⚠️ No valid hostel address for room ID: {}", room.getRoomId());
        }

        return dto;
    }
}