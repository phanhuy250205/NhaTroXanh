package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.HostelDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.AddressRepository;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
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
    @Transactional
    public Hostel createHostel(HostelDTO dto) {
        System.out.println("Creating hostel with DTO: " + dto);
        Users owner = usersRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ trọ"));

        String combinedAddress = dto.getCombinedAddress();
        System.out.println("Saving address: " + combinedAddress);

        Address address = new Address();
        address.setStreet(combinedAddress);
        address.setUser(owner);
        address = addressRepository.save(address);

        Hostel hostel = Hostel.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .room_number(dto.getRoomNumber())
                .createdAt(new java.sql.Date(System.currentTimeMillis()))
                .address(address)
                .owner(owner)
                .build();

        System.out.println("Saving hostel: " + hostel);
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
            address.setStreet(hostelDTO.getCombinedAddress());
            addressRepository.save(address);
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
        Address address = Address.builder()
                .street(hostelDTO.getCombinedAddress())
                .build();
        return addressRepository.save(address);
    }
}