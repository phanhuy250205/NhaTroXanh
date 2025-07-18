package nhatroxanh.com.Nhatroxanh.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;
import nhatroxanh.com.Nhatroxanh.Repository.PaymentsRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

    private final PaymentsRepository paymentsRepository;

    /**
     * Chạy mỗi ngày lúc 2 giờ sáng để cập nhật trạng thái thanh toán quá hạn
     * Runs daily at 2 AM to update overdue payment status
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void updateOverduePayments() {
        try {
            log.info("Starting scheduled task to update overdue payments");
            
            // Lấy ngày hiện tại theo múi giờ Việt Nam
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            Date currentDate = Date.valueOf(today);
            
            // Tìm tất cả các payment có trạng thái CHƯA_THANH_TOÁN và đã quá hạn
            List<Payments> unpaidPayments = paymentsRepository.findUnpaidPaymentsPastDueDate(currentDate);
            
            if (unpaidPayments.isEmpty()) {
                log.info("No overdue payments found for date: {}", currentDate);
                return;
            }
            
            int updatedCount = 0;
            
            for (Payments payment : unpaidPayments) {
                try {
                    // Cập nhật trạng thái thành QUÁ_HẠN_THANH_TOÁN
                    payment.setPaymentStatus(Payments.PaymentStatus.QUÁ_HẠN_THANH_TOÁN);
                    paymentsRepository.save(payment);
                    updatedCount++;
                    
                    log.debug("Updated payment {} to OVERDUE status. Due date: {}, Current date: {}", 
                            payment.getId(), payment.getDueDate(), currentDate);
                            
                } catch (Exception e) {
                    log.error("Failed to update payment {} to overdue status: {}", 
                            payment.getId(), e.getMessage(), e);
                }
            }
            
            log.info("Successfully updated {} payments to overdue status out of {} found", 
                    updatedCount, unpaidPayments.size());
                    
        } catch (Exception e) {
            log.error("Error in scheduled task to update overdue payments: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Chạy mỗi tuần vào Chủ nhật lúc 3 giờ sáng để thống kê và log
     * Runs weekly on Sunday at 3 AM for statistics and logging
     */
    @Scheduled(cron = "0 0 3 * * SUN", zone = "Asia/Ho_Chi_Minh")
    public void weeklyPaymentStatusReport() {
        try {
            log.info("Starting weekly payment status report");
            
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            Date currentDate = Date.valueOf(today);
            
            // Thống kê các loại payment
            long totalPayments = paymentsRepository.count();
            long paidPayments = paymentsRepository.countByPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
            long unpaidPayments = paymentsRepository.countByPaymentStatus(Payments.PaymentStatus.CHƯA_THANH_TOÁN);
            long overduePayments = paymentsRepository.countByPaymentStatus(Payments.PaymentStatus.QUÁ_HẠN_THANH_TOÁN);
            
            // Thống kê payment quá hạn trong tuần qua
            LocalDate weekAgo = today.minusDays(7);
            Date weekAgoDate = Date.valueOf(weekAgo);
            long newOverdueThisWeek = paymentsRepository.countNewOverduePaymentsSince(weekAgoDate);
            
            log.info("=== WEEKLY PAYMENT STATUS REPORT ===");
            log.info("Report Date: {}", currentDate);
            log.info("Total Payments: {}", totalPayments);
            log.info("Paid Payments: {} ({:.1f}%)", paidPayments, 
                    totalPayments > 0 ? (paidPayments * 100.0 / totalPayments) : 0);
            log.info("Unpaid Payments: {} ({:.1f}%)", unpaidPayments, 
                    totalPayments > 0 ? (unpaidPayments * 100.0 / totalPayments) : 0);
            log.info("Overdue Payments: {} ({:.1f}%)", overduePayments, 
                    totalPayments > 0 ? (overduePayments * 100.0 / totalPayments) : 0);
            log.info("New Overdue This Week: {}", newOverdueThisWeek);
            log.info("=====================================");
            
        } catch (Exception e) {
            log.error("Error in weekly payment status report: {}", e.getMessage(), e);
        }
    }
}
