package nhatroxanh.com.Nhatroxanh.Config;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.entity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Repository.VoucherRepository;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class VoucherScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VoucherScheduler.class);

    private final VoucherRepository voucherRepository;
    private final EmailService emailService;

    public VoucherScheduler(VoucherRepository voucherRepository, EmailService emailService) {
        this.voucherRepository = voucherRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 */1 * * * *") // chạy mỗi phút
    @Transactional
    public void autoDisableExpiredOrEmptyVouchers() {
        logger.info("🔁 [VoucherScheduler] Bắt đầu kiểm tra voucher hết hạn hoặc hết số lượng...");

        List<Vouchers> vouchers = voucherRepository.findByStatus(true);
        LocalDate today = LocalDate.now();

        for (Vouchers voucher : vouchers) {
            // Tránh NullPointerException
            LocalDate endDate = voucher.getEndDate() != null ? voucher.getEndDate().toLocalDate() : null;
            Integer quantity = voucher.getQuantity();

            boolean isExpired = endDate != null && !endDate.isAfter(today); // endDate <= today
            boolean isOutOfQuantity = quantity == null || quantity == 0;

            if (isExpired || isOutOfQuantity) {
                logger.info("⚠️ Voucher [{}] hết hạn hoặc hết số lượng. Đang cập nhật trạng thái...",
                        voucher.getCode());

                voucher.setStatus(false);
                voucherRepository.save(voucher);

                // Gửi email thông báo
                try {
                    emailService.sendVoucherDeactivationEmail(voucher);
                    logger.info("📧 Đã gửi mail thông báo vô hiệu hóa cho voucher [{}]", voucher.getCode());
                } catch (Exception e) {
                    logger.error("❌ Lỗi khi gửi email cho voucher [{}]: {}", voucher.getCode(), e.getMessage());
                }
            }
        }

        logger.info("✅ [VoucherScheduler] Đã kiểm tra và xử lý tất cả voucher.");
    }

}
