package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;
import java.util.Optional;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;

// ✅ Interface KHÔNG có @Service và @Autowired
public interface RoomsService {
    List<Rooms> findAllRooms();
    List<ContractDto.Room> getRoomsByOwnerId(Integer ownerId);
    List<ContractDto.Room> getRoomsByHostelId(Integer hostelId);
    List<Rooms> findByHostelId(Integer hostelId); // ✅ THÊM METHOD NÀY
    Optional<Rooms> findById(Integer id);
    Rooms save(Rooms room);

}
