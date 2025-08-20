package nhatroxanh.com.Nhatroxanh.Service;

import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.entity.Notification;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ScheduledTasksService {

    @Autowired
    private ContractsRepository contractsRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    /**
     * Scheduled task to automatically cleanup notifications older than 10 days
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("🧹 Bắt đầu tác vụ tự động xóa thông báo cũ hơn 10 ngày...");

        try {
            // Gọi phương thức cleanupOldNotifications từ NotificationService
            int deletedCount = notificationService.cleanupOldNotifications();

            if (deletedCount > 0) {
                log.info("✅ Đã xóa thành công {} thông báo cũ hơn 10 ngày.", deletedCount);
            } else {
                log.info("ℹ️ Không tìm thấy thông báo nào cũ hơn 10 ngày để xóa.");
            }

        } catch (Exception e) {
            log.error("❌ Lỗi khi thực hiện tác vụ xóa thông báo cũ: {}", e.getMessage(), e);
        }

        log.info("🎉 Hoàn thành tác vụ xóa thông báo cũ.");
    }

}
