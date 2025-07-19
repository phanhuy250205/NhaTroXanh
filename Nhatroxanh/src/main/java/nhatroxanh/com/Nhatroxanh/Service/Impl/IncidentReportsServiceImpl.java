package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports.IncidentStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.IncidentReportsRepository;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import nhatroxanh.com.Nhatroxanh.Service.IncidentReportsService;

import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class IncidentReportsServiceImpl implements IncidentReportsService {

    @Autowired
    private IncidentReportsRepository incidentRepository;

    @Autowired
    private EmailService mailService;

    @Override
    public Page<IncidentReports> getIncidentsByHostId(Integer hostId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (search != null && !search.trim().isEmpty()) {
            return incidentRepository.findByHostIdAndSearch(
                    hostId,
                    "%" + search.toLowerCase() + "%",
                    pageable);
        }
        return incidentRepository.findByHostId(hostId, pageable);
    }

    @Override
    public IncidentReports getIncidentById(Integer id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại với ID: " + id));
    }

    @Override
    @Transactional
    public void updateIncident(Integer id, IncidentReports updatedIncident) {
        IncidentReports incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự cố"));

        IncidentStatus oldStatus = incident.getStatus();
        IncidentStatus newStatus = updatedIncident.getStatus();

        incident.setStatus(newStatus);

        // Lấy email từ người gửi sự cố
        Users user = incident.getUser(); // hoặc getCreatedBy() tùy bạn dùng cái nào
        String email = (user != null) ? user.getEmail() : null;

        // CHUA_XU_LY ➝ DANG_XU_LY → set thời gian và gửi email "đang xử lý"
        if (oldStatus == IncidentStatus.CHUA_XU_LY && newStatus == IncidentStatus.DANG_XU_LY) {
            incident.setResolvedAt(new Date(System.currentTimeMillis()));

            if (email != null) {
                try {
                    mailService.sendIncidentProcessingEmail(email, incident);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // DANG_XU_LY ➝ DA_XU_LY → gửi email "đã xử lý"
        if (oldStatus == IncidentStatus.DANG_XU_LY && newStatus == IncidentStatus.DA_XU_LY) {
            if (email != null) {
                try {
                    mailService.sendIncidentResolvedEmail(email, incident);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Hoặc log lỗi gửi email
                }
            }
        }

        incidentRepository.save(incident);
    }

}
