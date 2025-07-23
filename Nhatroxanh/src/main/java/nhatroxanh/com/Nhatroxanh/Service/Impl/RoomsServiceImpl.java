package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.Ward;
import nhatroxanh.com.Nhatroxanh.Model.enity.District;
import nhatroxanh.com.Nhatroxanh.Model.enity.Province;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import nhatroxanh.com.Nhatroxanh.Util.AddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomsServiceImpl implements RoomsService {

    private static final Logger logger = LoggerFactory.getLogger(RoomsServiceImpl.class);

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private RoomsRepository roomsRepository;

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
    public List<ContractDto.Room> getRoomsByHostelId(Integer hostelId) {
        logger.info("Fetching rooms for hostelId: {}", hostelId);
        if (hostelId == null) {
            logger.error("hostelId is null");
            return new ArrayList<>();
        }

        List<Rooms> rooms = roomsRepository.findByHostelId(hostelId);
        logger.info("Raw rooms from database: {}", rooms.stream().map(r -> {
            return "roomId=" + r.getRoomId() + ", address=" + (r.getAddress() != null ? r.getAddress() : "null") +
                    ", entityClass=" + r.getClass().getName();
        }).collect(Collectors.toList()));
        List<ContractDto.Room> result = rooms.stream()
                .map(this::convertToRoomDto)
                .collect(Collectors.toList());
        logger.info("Returning rooms with addresses: {}", result.stream().map(r -> {
            return "roomId=" + r.getRoomId() + ", address=" + (r.getAddress() != null ? r.getAddress() : "null") +
                    ", dtoClass=" + r.getClass().getName();
        }).collect(Collectors.toList()));
        return result;
    }

    // @Override
    // public List<Rooms> findByHostelId(Integer hostelId) {
    //     return roomsRepository.findByHostel_HostelId(hostelId);
    // }

    @Override
    public Rooms save(Rooms room) {
        logger.info("Saving room: {}", room.getNamerooms());
        if (room == null) {
            logger.error("Room is null");
            throw new IllegalArgumentException("Phòng không được null!");
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
        return roomsRepository.findById(id);
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
        roomDto.setStatus(room.getStatus() != null ? room.getStatus().name() : null);
        roomDto.setHostelId(room.getHostel() != null ? room.getHostel().getHostelId() : null);
        roomDto.setHostelName(room.getHostel() != null ? room.getHostel().getName() : null);

        // Xử lý địa chỉ từ cột address của Rooms
        String roomAddress = room.getAddress();
        logger.info("Address for room with roomId {}: {}", room.getRoomId(), roomAddress);

        if (StringUtils.hasText(roomAddress)) {
            Map<String, String> addressParts = AddressUtils.parseAddress(roomAddress);
            roomDto.setStreet(addressParts.getOrDefault("street", ""));
            roomDto.setWard(addressParts.getOrDefault("ward", ""));
            roomDto.setDistrict(addressParts.getOrDefault("district", ""));
            roomDto.setProvince(addressParts.getOrDefault("province", ""));
            roomDto.setAddress(roomAddress); // Lưu toàn bộ chuỗi địa chỉ
        } else {
            logger.warn("No address found for room with roomId: {}", room.getRoomId());
            roomDto.setStreet("");
            roomDto.setWard("");
            roomDto.setDistrict("");
            roomDto.setProvince("");
            roomDto.setAddress("");
        }

        logger.info("Mapped room: {}", roomDto);
        return roomDto;
    }

    @Override
    public List<Rooms> findByHostelId(Integer hostelId) {
         return roomsRepository.findByHostel_HostelId(hostelId);
    }


}