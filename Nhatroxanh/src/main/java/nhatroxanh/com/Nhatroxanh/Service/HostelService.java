package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HostelService {

    @Autowired
    private HostelRepository hostelRepository;

    // Lấy tất cả khu trọ
    public List<Hostel> findAllHostels() {
        return hostelRepository.findAllWithDetails();
    }

    // Lấy khu trọ theo ID
    public Optional<Hostel> findHostelById(Integer id) {
        return hostelRepository.findById(id);
    }

    // Lưu hoặc cập nhật khu trọ
    public Hostel saveHostel(Hostel hostel) {
        return hostelRepository.save(hostel);
    }

    // Xóa khu trọ
    public void deleteHostel(Integer id) {
        hostelRepository.deleteById(id);
    }
}