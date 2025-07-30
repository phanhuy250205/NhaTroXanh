package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.entity.Address;
import nhatroxanh.com.Nhatroxanh.Model.entity.District;
import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Province;
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
        if (room.getHostel() != null && room.getHostel().getAddress() != null) {
            Address address = room.getHostel().getAddress();
            List<String> addressParts = new ArrayList<>();
            if (address.getStreet() != null) {
                addressParts.add(address.getStreet());
            }
            if (address.getWard() != null) {
                addressParts.add(address.getWard().getName());
                if (address.getWard().getDistrict() != null) {
                    addressParts.add(address.getWard().getDistrict().getName());
                    if (address.getWard().getDistrict().getProvince() != null) {
                        addressParts.add(address.getWard().getDistrict().getProvince().getName());
                    }
                }
            }
            String fullAddress = addressParts.isEmpty() ? "" : String.join(", ", addressParts);
            roomDto.setAddress(fullAddress);
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
        return roomsRepository.findByHostelId(hostelId);
    }
}