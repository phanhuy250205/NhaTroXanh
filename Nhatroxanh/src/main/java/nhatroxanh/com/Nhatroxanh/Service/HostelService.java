package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HostelService {

    @Autowired
    private HostelRepository hostelRepository;

    @Transactional
    public void saveHostel(Hostel hostel) {
        hostelRepository.save(hostel);
    }

    public Optional<Hostel> findHostelById(Integer id) {
        return hostelRepository.findByIdWithAddress(id);
    }

    public List<Hostel> findAllHostels() {
        return hostelRepository.findAllWithDetails();
    }

    public List<Hostel> findHostelsByName(String name) {
        return hostelRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public void deleteHostelById(Integer id) {
        hostelRepository.deleteById(id);
    }
}