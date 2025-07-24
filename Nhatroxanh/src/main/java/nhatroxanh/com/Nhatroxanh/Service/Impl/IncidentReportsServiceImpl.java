package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports.IncidentStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Repository.IncidentReportsRepository;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import nhatroxanh.com.Nhatroxanh.Service.IncidentReportsService;
import nhatroxanh.com.Nhatroxanh.Service.NotificationService;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class IncidentReportsServiceImpl implements IncidentReportsService {

    @Autowired
    private IncidentReportsRepository incidentRepository;

    @Autowired
    private EmailService mailService;

    @Autowired
    private NotificationService notificationService;

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
    public IncidentReports getIncidentById(Integer reportId) {
        return incidentRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại với ID: " + reportId));
    }

    @Override
    @Transactional
    public void updateIncident(Integer reportId, IncidentReports updatedIncident) {
        IncidentReports incident = incidentRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự cố với ID: " + reportId));

        IncidentStatus oldStatus = incident.getStatus();
        IncidentStatus newStatus = updatedIncident.getStatus();

        incident.setStatus(newStatus);

        // Get user and email
        Users user = incident.getUser();
        String email = (user != null) ? user.getEmail() : null;

        // Get room information
        Rooms room = incident.getRoom();

        // CHUA_XU_LY ➝ DANG_XU_LY → set resolvedAt, send email, and create notification
        if (oldStatus == IncidentStatus.CHUA_XU_LY && newStatus == IncidentStatus.DANG_XU_LY) {
            incident.setResolvedAt(new Date(System.currentTimeMillis()));

            if (email != null) {
                try {
                    mailService.sendIncidentProcessingEmail(email, incident);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Create notification for "Đang xử lý"
            String title = "Sự cố đang được xử lý";
            String message = String.format("Sự cố #%d (%s, mức độ: %s) tại phòng %s đang được xử lý. Chúng tôi sẽ cập nhật thêm thông tin sớm.",
                    incident.getReportId(),
                    incident.getIncidentType(),
                    incident.getLevel().toString(),
                    room != null ? room.getNamerooms() : "N/A");
            notificationService.createIncidentNotification(user, incident, title, message);
        }

        // DANG_XU_LY ➝ DA_XU_LY → send email and create notification
        if (oldStatus == IncidentStatus.DANG_XU_LY && newStatus == IncidentStatus.DA_XU_LY) {
            if (email != null) {
                try {
                    mailService.sendIncidentResolvedEmail(email, incident);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Create notification for "Đã xử lý"
            String title = "Sự cố đã được xử lý";
            String message = String.format("Sự cố #%d (%s, mức độ: %s) tại phòng %s đã được giải quyết. Vui lòng kiểm tra và liên hệ nếu cần thêm hỗ trợ.",
                    incident.getReportId(),
                    incident.getIncidentType(),
                    incident.getLevel().toString(),
                    room != null ? room.getNamerooms() : "N/A");
            notificationService.createIncidentNotification(user, incident, title, message);
        }

        // CHUA_XU_LY ➝ DA_XU_LY → direct resolution, send email and create notification
        if (oldStatus == IncidentStatus.CHUA_XU_LY && newStatus == IncidentStatus.DA_XU_LY) {
            incident.setResolvedAt(new Date(System.currentTimeMillis()));

            if (email != null) {
                try {
                    mailService.sendIncidentResolvedEmail(email, incident);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Create notification for direct resolution
            String title = "Sự cố đã được xử lý";
            String message = String.format("Sự cố #%d (%s, mức độ: %s) tại phòng %s đã được giải quyết trực tiếp. Vui lòng kiểm tra và liên hệ nếu cần thêm hỗ trợ.",
                    incident.getReportId(),
                    incident.getIncidentType(),
                    incident.getLevel().toString(),
                    room != null ? room.getNamerooms() : "N/A");
            notificationService.createIncidentNotification(user, incident, title, message);
        }

        incidentRepository.save(incident);
    }
}