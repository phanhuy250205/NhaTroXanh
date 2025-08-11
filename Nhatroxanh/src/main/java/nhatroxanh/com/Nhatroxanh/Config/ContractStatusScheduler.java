package nhatroxanh.com.Nhatroxanh.Config;

import nhatroxanh.com.Nhatroxanh.Model.entity.Contracts;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Component
public class ContractStatusScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ContractStatusScheduler.class);

    @Autowired
    private ContractsRepository contractsRepository;

    /**
     * ✅ Cập nhật hợp đồng SẮPHET HẠN (còn <= 3 ngày)
     * Chạy mỗi ngày lúc 0:00.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Chạy lúc 0:00 mỗi ngày
    public void updateExpiredContracts() {
        logger.info("🔄 Bắt đầu kiểm tra hợp đồng sắp hết hạn");

        try {
            LocalDate today = LocalDate.now();
            LocalDate expireThreshold = today.plusDays(3); // Còn 3 ngày

            Date sqlToday = Date.valueOf(today);
            Date sqlExpireThreshold = Date.valueOf(expireThreshold);

            // ✅ Tìm hợp đồng ACTIVE sắp hết hạn (endDate <= today + 3 ngày)
            List<Contracts> expiringContracts = contractsRepository
                    .findByStatusAndEndDateBetween(
                            Contracts.Status.ACTIVE,
                            sqlToday,
                            sqlExpireThreshold
                    );

            // ✅ Cập nhật thành EXPIRED
            for (Contracts contract : expiringContracts) {
                contract.setStatus(Contracts.Status.EXPIRED);
                contractsRepository.save(contract);
                logger.info("✅ Cập nhật hợp đồng ID {} thành EXPIRED (còn {} ngày)",
                        contract.getContractId(),
                        java.time.temporal.ChronoUnit.DAYS.between(today, contract.getEndDate().toLocalDate())
                );
            }

            logger.info("🎯 Hoàn tất! Đã cập nhật {} hợp đồng thành EXPIRED", expiringContracts.size());

        } catch (Exception e) {
            logger.error("❌ Lỗi khi cập nhật trạng thái hợp đồng: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ Cập nhật hợp đồng ĐÃ HẾT HẠN thành TERMINATED
     */
    @Scheduled(cron = "0 30 0 * * ?") // Chạy lúc 0:30 mỗi ngày
    public void updateTerminatedContracts() {
        logger.info("🔄 Bắt đầu kiểm tra hợp đồng đã hết hạn");

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Date sqlYesterday = Date.valueOf(yesterday);

            // Tìm hợp đồng EXPIRED đã quá hạn
            List<Contracts> terminatedContracts = contractsRepository
                    .findByStatusAndEndDateLessThan(Contracts.Status.EXPIRED, sqlYesterday);

            for (Contracts contract : terminatedContracts) {
                contract.setStatus(Contracts.Status.TERMINATED);
                contractsRepository.save(contract);
                logger.info("✅ Cập nhật hợp đồng ID {} thành TERMINATED", contract.getContractId());
            }

            logger.info("🎯 Hoàn tất! Đã cập nhật {} hợp đồng thành TERMINATED", terminatedContracts.size());

        } catch (Exception e) {
            logger.error("❌ Lỗi khi cập nhật hợp đồng đã hết hạn: {}", e.getMessage(), e);
        }
    }
}
