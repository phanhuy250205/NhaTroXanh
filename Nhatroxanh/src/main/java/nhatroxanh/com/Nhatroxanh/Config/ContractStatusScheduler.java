package nhatroxanh.com.Nhatroxanh.Config;

import nhatroxanh.com.Nhatroxanh.Model.entity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.entity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.NotificationRepository;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import nhatroxanh.com.Nhatroxanh.Service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Component
public class ContractStatusScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ContractStatusScheduler.class);

    @Autowired
    private ContractsRepository contractsRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    /**
     * ✅ Cập nhật hợp đồng SẮPHET HẠN (còn <= 3 ngày)
     * Chạy mỗi ngày lúc 0:00.
     */
    @Scheduled(cron = "0 0 0 * * ?") 
    @Transactional
    public void updateContractsAndNotifyTenants() {
        logger.info("⏰ Bắt đầu quét hợp đồng còn <= 3 ngày để cập nhật & thông báo...");

        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(3); // còn 3 ngày nữa hết hạn

        List<Contracts> expiringContracts = contractsRepository.findByStatusAndEndDateLessThanEqual(
                Contracts.Status.ACTIVE, Date.valueOf(thresholdDate));

        logger.info("🔍 Tìm thấy {} hợp đồng sắp hết hạn.", expiringContracts.size());

        for (Contracts contract : expiringContracts) {
            // --- 1. Cập nhật trạng thái ---
            if (contract.getStatus() != Contracts.Status.EXPIRED) {
                contract.setStatus(Contracts.Status.EXPIRED);
                logger.info("✅ Hợp đồng ID {} được cập nhật thành EXPIRED.", contract.getContractId());
            }

            // --- 2. Gửi thông báo ---
            if (contract.getTenant() != null) {
                Users tenant = contract.getTenant();
                String uniqueMessageIdentifier = String.format("Hợp đồng thuê phòng %s của bạn sắp hết hạn",
                        contract.getRoom().getNamerooms());

                boolean alreadyNotified = notificationRepository.existsByUserAndMessageContaining(
                        tenant, uniqueMessageIdentifier);

                if (!alreadyNotified) {
                    // Tạo thông báo trong hệ thống
                    String title = "Hợp đồng của bạn sắp hết hạn";
                    String message = String.format(
                            "Hợp đồng thuê phòng %s của bạn sẽ hết hạn vào ngày %s. Vui lòng liên hệ chủ trọ để gia hạn nếu cần.",
                            contract.getRoom().getNamerooms(),
                            contract.getEndDate());

                    notificationService.createContractNotificationWithCleanup(tenant, contract.getRoom(), title,
                            message);
                    logger.info("📢 Đã tạo thông báo cho người thuê ID {}.", tenant.getUserId());

                    // --- 3. Gửi email ---
                    if (tenant.getEmail() != null && !tenant.getEmail().isEmpty()) {
                        try {
                            String emailSubject = "Thông báo: Hợp đồng thuê nhà của bạn sắp hết hạn";
                            String htmlTemplate = """
                                    <!DOCTYPE html>
                                    <html lang="vi">
                                    <head>
                                        <meta charset="UTF-8">
                                        <style>
                                            body { font-family: Arial, sans-serif; background-color: #f4f4f4; }
                                            .container { max-width: 600px; margin: 20px auto; background-color: #fff; border-radius: 8px; padding: 20px; }
                                            .header { background-color: #007bff; color: white; padding: 10px; text-align: center; }
                                            .content { padding: 20px; color: #333; }
                                            .info-box { background-color: #e9f5ff; padding: 15px; margin: 20px 0; border-left: 4px solid #007bff; }
                                            .footer { font-size: 12px; color: #777; text-align: center; margin-top: 20px; }
                                        </style>
                                    </head>
                                    <body>
                                        <div class="container">
                                            <div class="header"><h2>Thông báo Hợp đồng sắp hết hạn</h2></div>
                                            <div class="content">
                                                <p>Xin chào <strong>{{tenantName}}</strong>,</p>
                                                <p>Hợp đồng thuê phòng <strong>{{roomName}}</strong> sẽ hết hạn vào ngày <strong>{{endDate}}</strong>.</p>
                                                <div class="info-box">
                                                    Vui lòng liên hệ với chủ trọ để thảo luận về việc gia hạn nếu bạn muốn tiếp tục thuê.
                                                </div>
                                                <p>Trân trọng,<br>Đội ngũ Nhà Trọ Xanh</p>
                                            </div>
                                            <div class="footer">
                                                &copy; 2025 Nhà Trọ Xanh - Email tự động, vui lòng không trả lời.
                                            </div>
                                        </div>
                                    </body>
                                    </html>
                                    """;

                            String finalHtmlBody = htmlTemplate
                                    .replace("{{tenantName}}", tenant.getFullname())
                                    .replace("{{roomName}}", contract.getRoom().getNamerooms())
                                    .replace("{{endDate}}", contract.getEndDate().toString());

                            emailService.sendHtmlEmail(tenant.getEmail(), emailSubject, finalHtmlBody);
                            logger.info("📧 Email đã gửi đến {}.", tenant.getEmail());

                        } catch (Exception e) {
                            logger.error("❌ Lỗi khi gửi email cho {}: {}", tenant.getEmail(), e.getMessage());
                        }
                    }
                } else {
                    logger.info("⏩ Người thuê của hợp đồng ID {} đã được thông báo trước đó. Bỏ qua.",
                            contract.getContractId());
                }
            }
        }

        if (!expiringContracts.isEmpty()) {
            contractsRepository.saveAll(expiringContracts);
        }

        logger.info("🎉 Hoàn tất tác vụ cập nhật & thông báo cho hợp đồng sắp hết hạn.");
    }

    @Scheduled(cron = "0 0 0 * * ?") 
    @Transactional
    public void updateTerminatedContractsAndNotify() {
        logger.info("=== 🛑 Bắt đầu kiểm tra & xử lý hợp đồng quá hạn ===");

        try {
            LocalDate today = LocalDate.now();
            LocalDate expireThreshold = today.plusDays(3); // Còn 3 ngày

            Date sqlToday = Date.valueOf(today);
            Date sqlExpireThreshold = Date.valueOf(expireThreshold);

            // Lấy hợp đồng ACTIVE hoặc EXPIRED mà endDate < hôm nay
            List<Contracts> expiredContracts = contractsRepository
                    .findByStatusesAndEndDateBefore(
                            List.of(Contracts.Status.ACTIVE, Contracts.Status.EXPIRED),
                            sqlToday);

            if (expiredContracts.isEmpty()) {
                logger.info("Không có hợp đồng nào quá hạn hôm nay");
                return;
            }

            for (Contracts contract : expiredContracts) {
                contract.setStatus(Contracts.Status.TERMINATED);

                if (contract.getRoom() != null) {
                    contract.getRoom().setStatus(RoomStatus.unactive);
                }

                logger.info("❌ Hợp đồng ID {} đã quá hạn, chuyển TERMINATED & unactive phòng",
                        contract.getContractId());

                // =========================
                // 1️⃣ Gửi thông báo trên hệ thống
                // =========================
                if (contract.getTenant() != null) {
                    Users tenant = contract.getTenant();
                    String title = "Hợp đồng thuê phòng đã hết hạn";
                    String message = String.format(
                            "Hợp đồng thuê phòng %s của bạn đã hết hạn vào ngày %s.",
                            contract.getRoom().getNamerooms(),
                            contract.getEndDate());

                    notificationService.createContractNotificationWithCleanup(tenant, contract.getRoom(), title,
                            message);
                    logger.info("📢 Đã tạo thông báo trên hệ thống cho người thuê ID {}", tenant.getUserId());

                    // =========================
                    // 2️⃣ Gửi email HTML
                    // =========================
                    if (tenant.getEmail() != null && !tenant.getEmail().isEmpty()) {
                        try {
                            String emailSubject = "Thông báo: Hợp đồng thuê phòng đã hết hạn";

                            String htmlTemplate = """
                                        <html lang="vi">
                                        <head>
                                            <meta charset="UTF-8">
                                            <style>
                                                body { font-family: Arial, sans-serif; background-color: #f4f4f4; }
                                                .container { max-width: 600px; margin: 20px auto; background: white; padding: 20px; border-radius: 8px; }
                                                h2 { color: #dc3545; }
                                                .info-box { background-color: #ffe5e5; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; }
                                            </style>
                                        </head>
                                        <body>
                                            <div class="container">
                                                <h2>Thông báo Hợp đồng đã hết hạn</h2>
                                                <p>Xin chào <strong>{{tenantName}}</strong>,</p>
                                                <p>Hợp đồng thuê phòng của bạn đã hết hạn vào ngày <strong>{{endDate}}</strong>.</p>
                                                <div class="info-box">
                                                    <p><strong>Phòng:</strong> {{roomName}}</p>
                                                    <p><strong>Trạng thái:</strong> Đã chấm dứt</p>
                                                </div>
                                                <p>Vui lòng liên hệ chủ trọ nếu cần gia hạn hoặc ký hợp đồng mới.</p>
                                                <p>Trân trọng,<br>Nhà Trọ Xanh</p>
                                            </div>
                                        </body>
                                        </html>
                                    """;

                            String finalHtmlBody = htmlTemplate
                                    .replace("{{tenantName}}", tenant.getFullname())
                                    .replace("{{roomName}}", contract.getRoom().getNamerooms())
                                    .replace("{{endDate}}", contract.getEndDate().toString());

                            emailService.sendHtmlEmail(tenant.getEmail(), emailSubject, finalHtmlBody);
                            logger.info("✅ Đã gửi email hết hạn hợp đồng đến {}", tenant.getEmail());

                        } catch (Exception e) {
                            logger.error("❌ Lỗi gửi email hết hạn hợp đồng: {}", e.getMessage(), e);
                        }
                    }
                }
            }

            contractsRepository.saveAll(expiredContracts);
            logger.info("🎯 Đã xử lý & thông báo {} hợp đồng quá hạn", expiredContracts.size());

        } catch (Exception e) {
            logger.error("⚠️ Lỗi khi xử lý hợp đồng quá hạn: {}", e.getMessage(), e);
        }

        logger.info("=== ✅ Hoàn tất xử lý hợp đồng quá hạn ===");
    }

}
