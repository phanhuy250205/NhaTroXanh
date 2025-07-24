package nhatroxanh.com.Nhatroxanh.Service;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;

@Service
public class UtilityService {

    @Autowired
    private UtilityRepository utilityRepository;

    @Transactional
    public Utility createUtility(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tiện ích không được để trống");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Tên tiện ích không được vượt quá 100 ký tự");
        }

        // Check for duplicate
        if (utilityRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
            throw new IllegalArgumentException("Tiện ích \"" + name + "\" đã tồn tại");
        }

        Utility utility = Utility.builder()
                .name(name.trim())
                .build();

        return utilityRepository.save(utility);
    }

    @Transactional
    public void deleteUtility(Integer utilityId) {
        if (!utilityRepository.existsById(utilityId)) {
            throw new IllegalArgumentException("Tiện ích không tồn tại với ID: " + utilityId);
        }
        utilityRepository.deleteById(utilityId);
    }
    public List<Utility> findAll() {
        return utilityRepository.findAll();
    }

    public List<Utility> searchUtilities(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return utilityRepository.findAll();
        }
        return utilityRepository.searchByName(keyword);
    }

    public Utility addUtility(Utility utility) throws Exception {
        if (utilityRepository.findByNameIgnoreCase(utility.getName()).isPresent()) {
            throw new Exception("Tiện ích với tên này đã tồn tại!");
        }
        return utilityRepository.save(utility);
    }

    public Utility updateUtility(Integer id, String name) throws Exception {
        Optional<Utility> existingUtility = utilityRepository.findById(id);
        if (!existingUtility.isPresent()) {
            throw new Exception("Tiện ích không tồn tại!");
        }
        Utility utility = existingUtility.get();
        if (!utility.getName().equals(name) && utilityRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new Exception("Tên tiện ích đã tồn tại!");
        }
        utility.setName(name);
        return utilityRepository.save(utility);
    }

    public void deleteUtilitystaff(Integer id) throws Exception {
        Optional<Utility> utility = utilityRepository.findById(id);
        if (!utility.isPresent()) {
            throw new Exception("Tiện ích không tồn tại!");
        }
        if (!utility.get().getPosts().isEmpty()) {
            throw new Exception("Không thể xóa tiện ích vì có bài đăng liên kết!");
        }
        utilityRepository.deleteById(id);
    }
}