package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.Utility;

// ✅ Interface KHÔNG có @Service và @Autowired
public interface RoomsService {
    List<Rooms> findAllRooms();
    List<ContractDto.Room> getRoomsByOwnerId(Integer ownerId);
    List<ContractDto.Room> getRoomsByHostelId(Integer hostelId); 
    Optional<Rooms> findById(Integer id);
    Rooms save(Rooms room);
    Set<Utility> getUtilitiesByRoomId(Integer roomId);
    List<Rooms> findByHostelId(Integer hostelId);

}