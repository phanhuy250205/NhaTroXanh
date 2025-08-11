 package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.HostelDTO;
import nhatroxanh.com.Nhatroxanh.Model.entity.Address;
import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Model.entity.Ward;
import nhatroxanh.com.Nhatroxanh.Repository.AddressRepository;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.WardRepository;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;

@Service
@Transactional
public class HostelServiceImpl implements HostelService {

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository usersRepository;

    @Override
    public List<Hostel> getHostelsByOwnerId(Integer ownerId) {
        return hostelRepository.findByOwner_UserId(ownerId);
    }

    @Override
    public Optional<Hostel> getHostelById(Integer id) {
        return hostelRepository.findById(id);
    }

    @Override
    public List<Hostel> searchHostelsByOwnerIdAndName(Integer ownerId, String keyword) {
        return hostelRepository.findByOwnerUserIdAndNameContainingIgnoreCase(ownerId, keyword);
    }

    @Override
    public void createHostel(HostelDTO hostelDTO) {
        Hostel hostel = new Hostel();
        hostel.setName(hostelDTO.getName());
        hostel.setDescription(hostelDTO.getDescription());
        hostel.setStatus(hostelDTO.getStatus());
        hostel.setRoom_number(hostelDTO.getRoomNumber());
        hostel.setCreatedAt(Date.valueOf(LocalDate.now()));
        hostel.setAddress(hostelDTO.getAddress());
        Users owner = new Users();
        owner.setUserId(hostelDTO.getOwnerId());
        hostel.setOwner(owner);
        hostelRepository.save(hostel);
    }

    @Override
    public void updateHostel(HostelDTO hostelDTO) {
        Hostel hostel = hostelRepository.findById(hostelDTO.getHostelId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu trọ"));
        hostel.setName(hostelDTO.getName());
        hostel.setDescription(hostelDTO.getDescription());
        hostel.setStatus(hostelDTO.getStatus());
        hostel.setRoom_number(hostelDTO.getRoomNumber());
        hostel.setAddress(hostelDTO.getAddress()); // Cập nhật chuỗi địa chỉ
        hostelRepository.save(hostel);
    }

    @Override
    public void deleteHostel(Integer hostelId) {
        hostelRepository.deleteById(hostelId);
    }

    @Override
    public int countByOwner(Users owner) {
        return hostelRepository.countByOwner(owner);
    }

    private Address createAddress(HostelDTO hostelDTO) {
        Address address = Address.builder()
                .street(hostelDTO.getCombinedAddress())
                .build();
        return addressRepository.save(address);
    }

    @Override
    public List<Hostel> getHostelsWithRoomsByOwnerId(Integer ownerId) {
        return hostelRepository.findHostelsWithRoomsByOwnerId(ownerId);
    }

}
