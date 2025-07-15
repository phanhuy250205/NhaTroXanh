package nhatroxanh.com.Nhatroxanh.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Notification;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createPaymentNotification(Users user, Payments payment, String title, String message) {
        Rooms room = null;
        if (payment != null && payment.getContract() != null && payment.getContract().getRoom() != null) {
            room = payment.getContract().getRoom();
        }

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(Notification.NotificationType.PAYMENT)
                .isRead(false)
                .createAt(Date.valueOf(LocalDate.now()))
                .room(room)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Updates existing payment reminder notifications to show payment success
     * and creates a new success notification
     */
    public void handlePaymentSuccess(Users user, Payments payment, String successTitle, String successMessage) {
        try {
            // Find existing payment reminder notifications for this payment
            List<Notification> existingNotifications = notificationRepository.findByUserUserIdAndTypeOrderByCreateAtDesc(
                    user.getUserId(), Notification.NotificationType.PAYMENT);
            
            String paymentIdStr = String.valueOf(payment.getId());
            
            // Update existing payment reminder notifications
            for (Notification notification : existingNotifications) {
                if (notification.getMessage() != null && notification.getMessage().contains("Hóa đơn #" + paymentIdStr)) {
                    // Check if this is a payment reminder (not already a success notification)
                    if (!notification.getTitle().contains("thành công")) {
                        // Update the existing notification to show success
                        String updatedMessage = updateMessageToSuccess(notification.getMessage(), payment);
                        notification.setTitle("Thanh toán thành công");
                        notification.setMessage(updatedMessage);
                        notification.setIsRead(false); // Mark as unread so user sees the update
                        notificationRepository.save(notification);
                        log.info("Updated existing payment notification {} to success status", notification.getNotificationId());
                        break; // Only update the first matching notification
                    }
                }
            }
            
            // Create a new success notification as well
            createPaymentNotification(user, payment, successTitle, successMessage);
            log.info("Created new payment success notification for user {} and payment {}", user.getUserId(), payment.getId());
            
        } catch (Exception e) {
            log.error("Error handling payment success notifications for user {} and payment {}: {}", 
                    user.getUserId(), payment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Updates a payment reminder message to show success status
     */
    private String updateMessageToSuccess(String originalMessage, Payments payment) {
        try {
            // Parse the original message to extract payment details
            Pattern pattern = Pattern.compile("Hóa đơn #(\\d+) cho tháng ([\\d/]+) \\(Tổng: ([^\\)]+)\\)\\. Hạn thanh toán: ([^\\.]+)\\.");
            Matcher matcher = pattern.matcher(originalMessage);
            
            if (matcher.find()) {
                String invoiceId = matcher.group(1);
                String month = matcher.group(2);
                String total = matcher.group(3);
                
                // Get room and hostel information
                String roomName = "N/A";
                String hostelName = "N/A";
                
                if (payment.getContract() != null && payment.getContract().getRoom() != null) {
                    roomName = payment.getContract().getRoom().getNamerooms();
                    if (payment.getContract().getRoom().getHostel() != null) {
                        hostelName = payment.getContract().getRoom().getHostel().getName();
                    }
                }
                
                // Create success message
                return String.format(
                    "Bạn đã thanh toán thành công hóa đơn #%s cho phòng %s tại %s. " +
                    "Tháng: %s. Số tiền: %s. Phương thức: ZaloPay. " +
                    "Cảm ơn bạn đã thanh toán đúng hạn!",
                    invoiceId, roomName, hostelName, month, total
                );
            }
        } catch (Exception e) {
            log.error("Error updating message to success: {}", e.getMessage(), e);
        }
        
        // Fallback: simple success message
        return "Thanh toán thành công! Cảm ơn bạn đã thanh toán đúng hạn.";
    }
}
