package nhatroxanh.com.Nhatroxanh.config;

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
     * Tác vụ định kỳ kiểm tra và cập nhật trạng thái hợp đồng thành EXPIRED.
     * Chạy mỗi ngày lúc 0:00.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Chạy lúc 0:00 mỗi ngày
    public void updateExpiredContracts() {
        logger.info("Bắt đầu kiểm tra hợp đồng hết hạn");

        try {
            LocalDate today = LocalDate.now();
            Date sqlToday = Date.valueOf(today);

            // Tìm tất cả hợp đồng ACTIVE có endDate trước hoặc bằng ngày hiện tại
            List<Contracts> expiringContracts = contractsRepository.findByStatusAndEndDateLessThanEqual(
                    Contracts.Status.ACTIVE, sqlToday);

            for (Contracts contract : expiringContracts) {
                contract.setStatus(Contracts.Status.EXPIRED);
                contractsRepository.save(contract);
                logger.info("Cập nhật hợp đồng ID {} thành EXPIRED", contract.getContractId());
            }

            logger.info("Hoàn tất kiểm tra hợp đồng hết hạn. Đã cập nhật {} hợp đồng", expiringContracts.size());

        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật trạng thái hợp đồng hết hạn: {}", e.getMessage(), e);
        }
    }
}