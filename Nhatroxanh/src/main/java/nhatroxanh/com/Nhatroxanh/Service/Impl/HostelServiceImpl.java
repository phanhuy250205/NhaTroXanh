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
    private UserRepository userRepository;

    @Autowired
    private WardRepository wardRepository;

    @Override
    public List<Hostel> getHostelsByOwnerId(Integer ownerId) {
        return hostelRepository.findByOwner_UserId(ownerId);
    }

    @Override
    public List<Hostel> getHostelsWithRoomsByOwnerId(Integer ownerId) {
        return hostelRepository.findHostelsWithRoomsByOwnerId(ownerId);
    }
    @Override
    public Optional<Hostel> getHostelById(Integer id) {
        return hostelRepository.findById(id);
    }

    @Override
    public Hostel createHostel(HostelDTO hostelDTO) {
        Address address = createAddress(hostelDTO);
        Users owner = userRepository.findById(hostelDTO.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ trọ"));

        Hostel hostel = Hostel.builder()
                .name(hostelDTO.getName())
                .description(hostelDTO.getDescription())
                .status(hostelDTO.getStatus())
                .room_number(hostelDTO.getRoomNumber())
                .createdAt(Date.valueOf(LocalDate.now()))
                .owner(owner)
                .address(address)
                .build();

        return hostelRepository.save(hostel);
    }

    @Override
    public Hostel updateHostel(HostelDTO hostelDTO) {
        Hostel existingHostel = hostelRepository.findById(hostelDTO.getHostelId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu trọ"));

        Address address = existingHostel.getAddress();
        if (address == null) {
            address = createAddress(hostelDTO);
        } else {
            updateAddress(address, hostelDTO);
        }

        existingHostel.setName(hostelDTO.getName());
        existingHostel.setDescription(hostelDTO.getDescription());
        existingHostel.setStatus(hostelDTO.getStatus());
        existingHostel.setRoom_number(hostelDTO.getRoomNumber());
        existingHostel.setAddress(address);

        return hostelRepository.save(existingHostel);
    }

    @Override
    public void deleteHostel(Integer hostelId) {
        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu trọ"));

        if (hostel.getAddress() != null) {
            addressRepository.delete(hostel.getAddress());
        }

        hostelRepository.delete(hostel);
    }

    @Override
    public int countByOwner(Users owner) {
        return hostelRepository.countByOwner(owner);
    }

    private Address createAddress(HostelDTO hostelDTO) {
        Ward ward = wardRepository.findById(Integer.parseInt(hostelDTO.getWard()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phường/xã"));

        String fullStreet = (hostelDTO.getHouseNumber() != null ? hostelDTO.getHouseNumber() + " " : "") +
                (hostelDTO.getStreet() != null ? hostelDTO.getStreet() : "");

        Address address = Address.builder()
                .street(fullStreet.trim())
                .ward(ward)
                .build();

        return addressRepository.save(address);
    }

    private void updateAddress(Address address, HostelDTO hostelDTO) {
        Ward ward = wardRepository.findById(Integer.parseInt(hostelDTO.getWard()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phường/xã"));

        String fullStreet = (hostelDTO.getHouseNumber() != null ? hostelDTO.getHouseNumber() + " " : "") +
                (hostelDTO.getStreet() != null ? hostelDTO.getStreet() : "");

        address.setStreet(fullStreet.trim());
        address.setWard(ward);
        addressRepository.save(address);
    }
}
