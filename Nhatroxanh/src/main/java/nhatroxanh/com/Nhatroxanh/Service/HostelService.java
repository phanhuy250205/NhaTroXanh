package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;
import java.util.Optional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.HostelDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

public interface  HostelService {

    List<Hostel> getHostelsByOwnerId(Integer ownerId);

    List<Hostel> getHostelsWithRoomsByOwnerId(Integer ownerId);

    Optional<Hostel> getHostelById(Integer id);

    Hostel createHostel(HostelDTO hostelDTO);

    Hostel updateHostel(HostelDTO hostelDTO);

    void deleteHostel(Integer hostelId);

    int countByOwner(Users owner);

    List<Hostel> searchHostelsByOwnerIdAndName(Integer ownerId, String keyword);
}
