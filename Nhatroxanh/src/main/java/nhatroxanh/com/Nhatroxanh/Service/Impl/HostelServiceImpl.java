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
import nhatroxanh.com.Nhatroxanh.Model.enity.District;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Province;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Ward;
import nhatroxanh.com.Nhatroxanh.Repository.AddressRepository;
import nhatroxanh.com.Nhatroxanh.Repository.DistrictRepository;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ProvinceRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.WardRepository;
import nhatroxanh.com.Nhatroxanh.Service.AddressService;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;

@Service
@Transactional
public class HostelServiceImpl implements HostelService {

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private AddressService addressService;

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
        // Lấy owner từ userId
        Users owner = usersRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ trọ"));

        // Tạo hoặc tìm Province
        Province province = provinceRepository.findByCode(dto.getProvinceCode())
                .orElseGet(() -> {
                    Province newProvince = Province.builder()
                            .code(dto.getProvinceCode())
                            .name(dto.getProvinceName())
                            .build();
                    return provinceRepository.save(newProvince);
                });

        // Tạo hoặc tìm District
        District district = districtRepository.findByCode(dto.getDistrictCode())
                .orElseGet(() -> {
                    District newDistrict = District.builder()
                            .code(dto.getDistrictCode())
                            .name(dto.getDistrictName())
                            .province(province)
                            .build();
                    return districtRepository.save(newDistrict);
                });

        // Tạo hoặc tìm Ward
        Ward ward = wardRepository.findByCode(dto.getWardCode())
                .orElseGet(() -> {
                    Ward newWard = Ward.builder()
                            .code(dto.getWardCode())
                            .name(dto.getWardName())
                            .district(district)
                            .build();
                    return wardRepository.save(newWard);
                });

        // Tạo Address
        Address address = new Address();
        address.setStreet((dto.getHouseNumber() != null ? dto.getHouseNumber() + " " : "") + (dto.getStreet() != null ? dto.getStreet() : "").trim());
        address.setUser(owner); // Gán owner làm user của Address
        address.setWard(ward);
        address = addressRepository.save(address);

        // Tạo Hostel
        Hostel hostel = Hostel.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .room_number(dto.getRoomNumber())
                .createdAt(new java.sql.Date(System.currentTimeMillis()))
                .address(address)
                .owner(owner)
                .ward(ward)
                .district(district)
                .province(province)
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
       Ward ward = wardRepository.findByCode(hostelDTO.getWardCode())
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
        Ward ward = wardRepository.findByCode(hostelDTO.getWardCode())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phường/xã"));

        String fullStreet = (hostelDTO.getHouseNumber() != null ? hostelDTO.getHouseNumber() + " " : "") +
                (hostelDTO.getStreet() != null ? hostelDTO.getStreet() : "");

        address.setStreet(fullStreet.trim());
        address.setWard(ward);
        addressRepository.save(address);
    }
}
