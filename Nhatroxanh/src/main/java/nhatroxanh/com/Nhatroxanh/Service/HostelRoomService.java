package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HostelRoomService {

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private RoomsRepository roomsRepository;

    public List<Hostel> getHostelsWithRoomsByOwnerId(Integer ownerId) {
        return hostelRepository.findHostelsWithRoomsByOwnerId(ownerId);
    }

    public List<ContractDto.Room> getRoomDtosByHostelId(Integer hostelId) {
        List<Rooms> rooms = roomsRepository.findRoomsWithDetailsByHostelId(hostelId);
        return rooms.stream().map(this::convertToRoomDto).collect(Collectors.toList());
    }

    private ContractDto.Room convertToRoomDto(Rooms room) {
        ContractDto.Room roomDto = new ContractDto.Room();
        roomDto.setRoomId(room.getRoomId());
        roomDto.setRoomName(room.getNamerooms());
        roomDto.setArea(room.getAcreage());
        roomDto.setPrice(room.getPrice());
        roomDto.setStatus(room.getStatus().name());
        roomDto.setHostelId(room.getHostel().getHostelId());
        roomDto.setHostelName(room.getHostel().getName());

        String hostelAddress = room.getHostel().getAddress();
        if (hostelAddress != null && !hostelAddress.isEmpty()) {
            roomDto.setAddress(hostelAddress);
        } else {
            roomDto.setAddress(""); // hoặc "Chưa cập nhật"
        }

        return roomDto;
    }
}
