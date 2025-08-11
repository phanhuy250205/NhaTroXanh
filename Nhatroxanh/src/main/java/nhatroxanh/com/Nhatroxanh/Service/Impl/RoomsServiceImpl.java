package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.entity.Address;
import nhatroxanh.com.Nhatroxanh.Model.entity.District;
import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Province;
import nhatroxanh.com.Nhatroxanh.Model.entity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.Utility;
import nhatroxanh.com.Nhatroxanh.Model.entity.Ward;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ImageRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import nhatroxanh.com.Nhatroxanh.Util.AddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomsServiceImpl implements RoomsService {

    private static final Logger logger = LoggerFactory.getLogger(RoomsServiceImpl.class);

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private RoomsRepository roomsRepository;

    @Autowired
    private UtilityRepository utilityRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Override
    public List<Rooms> findAllRooms() {
        return roomsRepository.findAll();
    }

    @Override
    public List<ContractDto.Room> getRoomsByOwnerId(Integer ownerId) {
        List<Hostel> hostels = hostelRepository.findHostelsWithRoomsByOwnerId(ownerId);
        return hostels.stream()
                .flatMap(hostel -> hostel.getRooms().stream())
                .map(this::convertToRoomDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ContractDto.Room> getRoomsByHostelId(Integer hostelId) {
        if (hostelId == null) {
            logger.warn("Hostel ID is null, returning empty list");
            return new ArrayList<>();
        }
        logger.info("Fetching rooms for hostelId: {}", hostelId);
        List<Rooms> rooms = roomsRepository.findByHostelId(hostelId);
        logger.debug("Found {} rooms in database", rooms.size());
        List<ContractDto.Room> result = rooms.stream()
                .map(this::convertToRoomDto)
                .collect(Collectors.toList());
        logger.debug("Returning {} rooms", result.size());
        return result;
    }

    // @Override
    // public List<Rooms> findByHostelId(Integer hostelId) {
    // return roomsRepository.findByHostel_HostelId(hostelId);
    // }

    @Override
    @Transactional
    public Rooms save(Rooms room) {
        logger.info("Saving room: {}", room.getNamerooms());
        if (room == null) {
            logger.error("Room is null");
            throw new IllegalArgumentException("Phòng không được null!");
        }

        // Nếu phòng là mới (roomId == null), đặt trạng thái mặc định là unactive
        if (room.getRoomId() == null && room.getStatus() == null) {
            logger.info("New room, setting default status to unactive");
            room.setStatus(RoomStatus.unactive);
        }

        // Nếu là cập nhật, kiểm tra chuyển trạng thái
        if (room.getRoomId() != null) {
            Optional<Rooms> existingRoomOpt = roomsRepository.findById(room.getRoomId());
            if (existingRoomOpt.isPresent()) {
                Rooms existingRoom = existingRoomOpt.get();
                // Ngăn chuyển từ active sang trạng thái khác
                if (existingRoom.getStatus() == RoomStatus.active && room.getStatus() != RoomStatus.active) {
                    logger.warn("Invalid status transition: from active to {} for roomId: {}",
                            room.getStatus(), room.getRoomId());
                    throw new IllegalArgumentException(
                            "Không thể thay đổi trạng thái phòng từ 'Đã thuê' sang trạng thái khác.");
                }
                // Ngăn chuyển từ unactive sang active
                if (existingRoom.getStatus() == RoomStatus.unactive &&
                        room.getStatus() != RoomStatus.unactive && room.getStatus() != RoomStatus.repair) {
                    logger.warn("Invalid status transition: from unactive to {} for roomId: {}",
                            room.getStatus(), room.getRoomId());
                    throw new IllegalArgumentException(
                            "Phòng ở trạng thái 'Trống' chỉ có thể chuyển sang 'Bảo trì'.");
                }
            }
        }

        return roomsRepository.save(room);
    }

    @Override
    public Optional<Rooms> findById(Integer id) {
        logger.info("Finding room by ID: {}", id);
        if (id == null) {
            logger.warn("Room ID is null");
            return Optional.empty();
        }
        return roomsRepository.findByIdWithUtilities(id);
    }

    @Override
    public Set<Utility> getUtilitiesByRoomId(Integer roomId) {
        logger.info("Fetching utilities for roomId: {}", roomId);
        if (roomId == null) {
            logger.warn("Room ID is null");
            return new HashSet<>();
        }
        return roomsRepository.findUtilitiesByRoomId(roomId);
    }

    public Rooms findRoomById(Integer roomId) {
        return roomsRepository.findById(roomId).orElse(null);
    }

    private ContractDto.Room convertToRoomDto(Rooms room) {
        ContractDto.Room roomDto = new ContractDto.Room();
        roomDto.setRoomId(room.getRoomId());
        roomDto.setRoomName(room.getNamerooms());
        roomDto.setArea(room.getAcreage());
        roomDto.setPrice(room.getPrice());
        roomDto.setStatus(room.getStatus() != null ? room.getStatus().name().toLowerCase() : "unactive");
        roomDto.setHostelId(room.getHostel() != null ? room.getHostel().getHostelId() : null);
        roomDto.setHostelName(room.getHostel() != null ? room.getHostel().getName() : null);
        roomDto.setMaxTenants(room.getMax_tenants());
        // Xử lý địa chỉ đầy đủ
        Hostel hostel = room.getHostel();
        if (hostel != null && hostel.getAddress() != null) {
            roomDto.setAddress(hostel.getAddress()); // Vì hostel.getAddress() là String
        } else {
            roomDto.setAddress(""); // hoặc bạn có thể ghi "Chưa cập nhật"
        }

        logger.info("Mapped room: {}", roomDto);
        return roomDto;
    }

    @Override
    public List<Rooms> findByHostelId(Integer hostelId) {
        return roomsRepository.findByHostelId(hostelId);
    }
}