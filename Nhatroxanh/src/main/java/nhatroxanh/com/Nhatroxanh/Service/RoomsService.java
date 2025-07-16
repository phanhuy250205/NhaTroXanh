package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
@Service
public interface RoomsService {


    List<Rooms> findAllRooms();

    List<ContractDto.Room> getRoomsByOwnerId(Integer ownerId);
    List<ContractDto.Room> getRoomsByHostelId(Integer hostelId); 
    Optional<Rooms> findById(Integer id);
    Rooms save(Rooms room);

    Set<Utility> getUtilitiesByRoomId(Integer roomId);

}
