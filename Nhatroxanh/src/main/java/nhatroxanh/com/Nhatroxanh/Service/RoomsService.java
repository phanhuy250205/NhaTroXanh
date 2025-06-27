package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
@Service
public class RoomsService {
    @Autowired
    private RoomsRepository roomsRepository;

    public List<Rooms> findAllRooms() {
        return roomsRepository.findAll();
    }

    public List<Rooms> findByStatus(Boolean status) {
        return roomsRepository.findByStatus(status);
    }

    public List<Rooms> findRoomsByHostelId(Integer hostelId) {
        return roomsRepository.findByHostel_HostelId(hostelId);
    }

    public List<Rooms> searchRoomsByName(String name) {
        return roomsRepository.findByNameroomsContainingIgnoreCase(name);
    }

    public Rooms findRoomById(Integer room_id) {
        return roomsRepository.findById(room_id).orElse(null);
    }

    public void saveRoom(Rooms room) {
        if (room == null) {
            throw new IllegalArgumentException("Room object cannot be null");
        }
        if (room.getHostel() == null) {
            throw new IllegalStateException("Hostel must be assigned to the room");
        }
        try {
            roomsRepository.save(room);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save room: " + e.getMessage(), e);
        }
    }
}
