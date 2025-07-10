package nhatroxanh.com.Nhatroxanh.Config;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Repository.VoucherRepository;

@Component
public class VoucherScheduler {

    @Autowired
    private VoucherRepository voucherRepository;

    // Chạy mỗi ngày lúc 1 giờ sáng
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Ho_Chi_Minh")
    public void updateExpiredAndZeroQuantityVouchers() {
        List<Vouchers> allVouchers = voucherRepository.findAll();

        Date today = Date.valueOf(LocalDate.now());

        for (Vouchers voucher : allVouchers) {
            boolean updated = false;

            // Nếu ngày hết hạn < hôm nay
            if (voucher.getEndDate() != null && voucher.getEndDate().before(today)) {
                if (Boolean.TRUE.equals(voucher.getStatus())) {
                    voucher.setStatus(false);
                    updated = true;
                }
            }

            // Nếu số lượng bằng 0
            if (voucher.getQuantity() != null && voucher.getQuantity() == 0) {
                if (Boolean.TRUE.equals(voucher.getStatus())) {
                    voucher.setStatus(false);
                    updated = true;
                }
            }

            if (updated) {
                voucherRepository.save(voucher);
            }
        }
    }
}