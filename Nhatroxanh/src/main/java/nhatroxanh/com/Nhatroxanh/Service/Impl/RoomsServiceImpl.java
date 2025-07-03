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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
        logger.info("Found {} rooms in database", rooms.size());
        List<ContractDto.Room> result = rooms.stream()
                .map(this::convertToRoomDto)
                .collect(Collectors.toList());
        logger.info("Returning {} rooms", result.size());
        return result;
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
            roomDto.setAddress("");
        }

        logger.info("Mapped room: {}", roomDto);
        return roomDto;
    }
}