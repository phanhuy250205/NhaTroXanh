package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HostelService {

    @Autowired
    private HostelRepository hostelRepository;

    public Optional<Hostel> findHostelById(Integer id) {
        return hostelRepository.findById(id);
    }

    public void saveHostel(Hostel hostel) {
        hostelRepository.save(hostel);
    }

    public void deleteHostelById(Integer id) {
        hostelRepository.deleteById(id);
    }

    // Get all hostels
    public List<Hostel> findAllHostels() {
        return hostelRepository.findAllWithDetails();
    }
    // Search hostels by name
    public List<Hostel> findHostelsByName(String name) {
        return hostelRepository.findByNameContainingIgnoreCase(name);
    }
}