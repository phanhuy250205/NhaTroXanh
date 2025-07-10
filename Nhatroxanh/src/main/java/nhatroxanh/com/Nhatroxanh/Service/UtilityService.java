package nhatroxanh.com.Nhatroxanh.Service;


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
}