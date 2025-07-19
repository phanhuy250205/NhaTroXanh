package nhatroxanh.com.Nhatroxanh.Config;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Repository.VoucherRepository;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;

@Component
public class VoucherScheduler {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private EmailService emailService;

    // Chạy mỗi ngày lúc 0h (theo múi giờ Việt Nam)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void checkAndUpdateVoucherStatus() {
        List<Vouchers> activeVouchers = voucherRepository.findByStatus(true);
        Date today = Date.valueOf(LocalDate.now());

        for (Vouchers voucher : activeVouchers) {
            boolean expired = voucher.getEndDate() != null && voucher.getEndDate().before(today);
            boolean outOfStock = voucher.getQuantity() != null && voucher.getQuantity() == 0;

            if (expired || outOfStock) {
                voucher.setStatus(false);
                voucherRepository.save(voucher);

                String reason = expired ? "Voucher đã hết hạn" : "Voucher đã hết số lượng";

                try {
                    emailService.sendVoucherDeactivatedEmail(
                            voucher.getUser().getEmail(),
                            voucher.getUser().getFullname(),
                            voucher.getTitle(),
                            reason);
                } catch (Exception e) {
                    System.err.println("Không thể gửi email cho voucher ID " + voucher.getId() + ": " + e.getMessage());
                }
            }
        }
    }
}
