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
    private EmailService emailService;

   // Trong file: ScheduledTasksService.java

@Scheduled(cron = "0 0 1 * * *") 
@Transactional
public void checkExpiringContractsAndNotifyTenants() {
    log.info("⏰ Bắt đầu tác vụ quét hợp đồng sắp hết hạn để thông báo cho KHÁCH THUÊ...");

    LocalDate today = LocalDate.now();
    LocalDate sevenDaysLater = today.plusDays(3);

    List<Contracts> expiringContracts = contractsRepository.findByStatusAndEndDateBetween(
        Contracts.Status.ACTIVE,
        Date.valueOf(today),
        Date.valueOf(sevenDaysLater)
    );

    log.info("🔍 Tìm thấy {} hợp đồng sắp hết hạn.", expiringContracts.size());

    for (Contracts contract : expiringContracts) {
        if (contract.getTenant() != null) {
            Users tenant = contract.getTenant();
            String uniqueMessageIdentifier = String.format("Hợp đồng thuê phòng %s của bạn sắp hết hạn", contract.getRoom().getNamerooms());
            boolean alreadyNotified = notificationRepository.existsByUserAndMessageContaining(
                tenant, uniqueMessageIdentifier
            );

            if (!alreadyNotified) {
                // --- PHẦN TẠO THÔNG BÁO TRÊN CHUÔNG VẪN GIỮ NGUYÊN ---
                log.info("✅ Tạo thông báo trên chuông cho người thuê ID {}.", tenant.getUserId());
                Notification tenantNotification = new Notification();
                tenantNotification.setUser(tenant);
                tenantNotification.setTitle("Hợp đồng của bạn sắp hết hạn");
                String message = String.format("Hợp đồng thuê phòng %s của bạn sẽ hết hạn vào ngày %s. Vui lòng liên hệ chủ trọ để gia hạn nếu cần.",
                        contract.getRoom().getNamerooms(),
                        contract.getEndDate()
                );
                tenantNotification.setMessage(message);
                tenantNotification.setType(Notification.NotificationType.CONTRACT);
                tenantNotification.setRoom(contract.getRoom());
                tenantNotification.setIsRead(false);
                tenantNotification.setCreateAt(new Timestamp(System.currentTimeMillis()));
                notificationRepository.save(tenantNotification);
                log.info("✅ Đã lưu thông báo trên chuông vào CSDL.");

                // ==========================================================
                // ✅ BẮT ĐẦU TẠO NỘI DUNG EMAIL HTML
                // ==========================================================
                if (tenant.getEmail() != null && !tenant.getEmail().isEmpty()) {
                    try {
                        log.info("📧 Chuẩn bị gửi email HTML thông báo hết hạn đến: {}", tenant.getEmail());
                        String emailSubject = "Thông báo: Hợp đồng thuê nhà của bạn sắp hết hạn";
                        
                        // Mẫu email HTML
                        String htmlTemplate = """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
                                .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                                .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                                .header img { max-width: 150px; }
                                .content { padding: 30px; line-height: 1.6; color: #333333; }
                                .content h2 { color: #007bff; }
                                .info-box { background-color: #e9f5ff; border-left: 4px solid #007bff; padding: 15px; margin: 20px 0; }
                                .button { display: inline-block; background-color: #28a745; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 20px; }
                                .footer { background-color: #f8f9fa; color: #6c757d; text-align: center; padding: 20px; font-size: 12px; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <img src="/images/logo/nhatroxanh(title).png" alt="Nhà Trọ Xanh Logo">
                                </div>
                                <div class="content">
                                    <h2>Thông báo Hợp đồng sắp hết hạn</h2>
                                    <p>Xin chào <strong>{{tenantName}}</strong>,</p>
                                    <p>Hệ thống Nhà Trọ Xanh xin thông báo hợp đồng thuê nhà của bạn sắp đến ngày hết hạn.</p>
                                    <div class="info-box">
                                        <p><strong>Phòng trọ:</strong> {{roomName}}</p>
                                        <p><strong>Ngày hết hạn:</strong> <strong>{{endDate}}</strong></p>
                                    </div>
                                    <p>Vui lòng liên hệ với chủ trọ của bạn để thảo luận về việc gia hạn hợp đồng nếu bạn có nhu cầu tiếp tục thuê. </p>
                                    <p>Trân trọng,<br>Đội ngũ Nhà Trọ Xanh</p>
                                </div>
                                <div class="footer">
                                    <p>&copy; 2025 Nhà Trọ Xanh. All rights reserved.</p>
                                    <p>Đây là email tự động, vui lòng không trả lời.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """;

                        // Thay thế các placeholder bằng dữ liệu thực tế
                        String finalHtmlBody = htmlTemplate
                            .replace("{{tenantName}}", tenant.getFullname())
                            .replace("{{roomName}}", contract.getRoom().getNamerooms())
                            .replace("{{endDate}}", contract.getEndDate().toString());

                        // Gọi phương thức gửi email HTML
                        emailService.sendHtmlEmail(tenant.getEmail(), emailSubject, finalHtmlBody);

                        log.info("✅ Gửi email HTML thành công đến {}.", tenant.getEmail());

                    } catch (Exception e) {
                        log.error("❌ Lỗi khi gửi email HTML thông báo hết hạn cho {}: {}", tenant.getEmail(), e.getMessage());
                    }
                } else {
                    log.warn("⏩ Người thuê ID {} không có email. Bỏ qua việc gửi email.", tenant.getUserId());
                }
                // ==========================================================
                // ✅ KẾT THÚC PHẦN GỬI EMAIL
                // ==========================================================
            } else {
                log.info("⏩ Người thuê của hợp đồng ID {} đã được thông báo trước đó. Bỏ qua.", contract.getContractId());
            }
        }
    }
    log.info("🎉 Hoàn thành tác vụ quét hợp đồng cho khách thuê.");
  }
  

}
