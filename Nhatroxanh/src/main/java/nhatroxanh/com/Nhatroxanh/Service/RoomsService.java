package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;
import java.util.Optional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
@Service
public interface RoomsService {


    List<Rooms> findAllRooms();

    List<ContractDto.Room> getRoomsByOwnerId(Integer ownerId);
    List<ContractDto.Room> getRoomsByHostelId(Integer hostelId); // Thêm phương thức mới
    Optional<Rooms> findById(Integer id);
    Rooms save(Rooms room);

}
