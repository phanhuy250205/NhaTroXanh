package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
@Service
public class RoomsService {
    @Autowired
    private RoomsRepository roomsRepository;

    public List<Rooms> findAllRooms() {
        return roomsRepository.findAll();
    }
    
     
}
