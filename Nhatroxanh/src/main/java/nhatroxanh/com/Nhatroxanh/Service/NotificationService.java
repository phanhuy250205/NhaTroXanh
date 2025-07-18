package nhatroxanh.com.Nhatroxanh.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Notification;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Repository.NotificationRepository;
import nhatroxanh.com.Nhatroxanh.Repository.PaymentsRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PaymentsRepository paymentsRepository;

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
                .createAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
                .room(room)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Handles payment success by sending only ONE notification
     * Either updates existing reminder notification or creates a new success notification
     */
    public void handlePaymentSuccess(Users user, Payments payment, String successTitle, String successMessage) {
        try {
            if (user == null || payment == null) {
                log.error("Cannot handle payment success: user or payment is null");
                return;
            }

            // Find existing payment reminder notifications for this payment
            List<Notification> existingNotifications = notificationRepository.findByUserUserIdAndTypeOrderByCreateAtDesc(
                    user.getUserId(), Notification.NotificationType.PAYMENT);
            
            String paymentIdStr = String.valueOf(payment.getId());
            boolean notificationUpdated = false;
            
            // Try to update existing payment reminder notification
            for (Notification notification : existingNotifications) {
                if (notification.getMessage() != null && notification.getMessage().contains("Hóa đơn #" + paymentIdStr)) {
                    // Check if this is a payment reminder (not already a success notification)
                    if (!notification.getTitle().contains("thành công")) {
                        // Update the existing notification to show success
                        String updatedMessage = updateMessageToSuccess(notification.getMessage(), payment);
                        notification.setTitle(successTitle != null ? successTitle : "Thanh toán thành công");
                        notification.setMessage(updatedMessage);
                        notification.setIsRead(false); // Mark as unread so user sees the update
                        notificationRepository.save(notification);
                        log.info("Updated existing payment notification {} to success status", notification.getNotificationId());
                        notificationUpdated = true;
                        break; // Only update the first matching notification
                    }
                }
            }
            
            // If no existing notification was updated, create a new success notification
            if (!notificationUpdated) {
                createPaymentNotification(user, payment, successTitle, successMessage);
                log.info("Created new payment success notification for user {} and payment {}", user.getUserId(), payment.getId());
            }
            
        } catch (Exception e) {
            log.error("Error handling payment success notifications for user {} and payment {}: {}", 
                    user.getUserId(), payment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Updates a payment reminder message to show success status with proper data handling
     */
    private String updateMessageToSuccess(String originalMessage, Payments payment) {
        try {
            if (payment == null) {
                log.warn("Payment is null, using fallback message");
                return "Thanh toán thành công! Cảm ơn bạn đã thanh toán đúng hạn.";
            }

            // Get payment details with null checks
            String invoiceId = String.valueOf(payment.getId());
            String roomName = "N/A";
            String hostelName = "N/A";
            String paymentMethod = "N/A";
            String formattedAmount = "N/A";

            // Safely get contract and room information
            if (payment.getContract() != null) {
                if (payment.getContract().getRoom() != null) {
                    roomName = payment.getContract().getRoom().getNamerooms() != null 
                        ? payment.getContract().getRoom().getNamerooms() : "N/A";
                    
                    if (payment.getContract().getRoom().getHostel() != null) {
                        hostelName = payment.getContract().getRoom().getHostel().getName() != null
                            ? payment.getContract().getRoom().getHostel().getName() : "N/A";
                    }
                }
            }

            // Get payment method
            if (payment.getPaymentMethod() != null) {
                switch (payment.getPaymentMethod()) {
                    case VNPAY:
                        paymentMethod = "VNPay";
                        break;
                    case MOMO:
                        paymentMethod = "MoMo";
                        break;
                    default:
                        paymentMethod = payment.getPaymentMethod().toString();
                }
            }

            // Format amount
            if (payment.getTotalAmount() != null) {
                DecimalFormat formatter = new DecimalFormat("#,###");
                formattedAmount = formatter.format(payment.getTotalAmount()) + " VNĐ";
            }

            // Try to extract month from original message
            String month = "N/A";
            if (originalMessage != null) {
                Pattern pattern = Pattern.compile("cho tháng ([\\d/]+)");
                Matcher matcher = pattern.matcher(originalMessage);
                if (matcher.find()) {
                    month = matcher.group(1);
                }
            }

            // Create comprehensive success message
            return String.format(
                "Bạn đã thanh toán thành công hóa đơn #%s cho phòng %s tại %s. " +
                "Tháng: %s. Số tiền: %s. Phương thức: %s. " +
                "Cảm ơn bạn đã thanh toán đúng hạn!",
                invoiceId, roomName, hostelName, month, formattedAmount, paymentMethod
            );

        } catch (Exception e) {
            log.error("Error updating message to success: {}", e.getMessage(), e);
            
            // Enhanced fallback with basic payment info if available
            if (payment != null && payment.getId() != null) {
                return String.format("Thanh toán thành công hóa đơn #%d! Cảm ơn bạn đã thanh toán đúng hạn.", payment.getId());
            }
            
            return "Thanh toán thành công! Cảm ơn bạn đã thanh toán đúng hạn.";
        }
    }

    /**
     * Clean up obsolete payment notifications for a user
     * Removes notifications for payments that no longer exist or have been paid
     */
    public int cleanupObsoletePaymentNotifications(Integer userId) {
        try {
            log.info("Starting cleanup of obsolete payment notifications for user {}", userId);
            
            // Get all payment notifications for the user
            List<Notification> paymentNotifications = notificationRepository.findAllPaymentNotificationsByUserId(userId);
            List<Integer> notificationsToDelete = new ArrayList<>();
            
            for (Notification notification : paymentNotifications) {
                Integer paymentId = extractPaymentIdFromMessage(notification.getMessage());
                
                if (paymentId != null) {
                    // Check if payment still exists
                    if (!paymentsRepository.existsByPaymentId(paymentId)) {
                        // Payment doesn't exist anymore, mark for deletion
                        notificationsToDelete.add(notification.getNotificationId());
                        log.debug("Marking notification {} for deletion - payment {} no longer exists", 
                                notification.getNotificationId(), paymentId);
                    } else {
                        // Payment exists, check if it's paid and this is a reminder notification
                        Optional<Payments.PaymentStatus> statusOpt = paymentsRepository.findPaymentStatusById(paymentId);
                        if (statusOpt.isPresent() && statusOpt.get() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                            // Payment is paid, check if this is a reminder notification (not success notification)
                            if (!notification.getTitle().contains("thành công") && 
                                !notification.getMessage().contains("thanh toán thành công")) {
                                notificationsToDelete.add(notification.getNotificationId());
                                log.debug("Marking notification {} for deletion - payment {} is already paid", 
                                        notification.getNotificationId(), paymentId);
                            }
                        }
                    }
                } else {
                    log.warn("Could not extract payment ID from notification {}: {}", 
                            notification.getNotificationId(), notification.getMessage());
                }
            }
            
            // Delete obsolete notifications
            if (!notificationsToDelete.isEmpty()) {
                notificationRepository.deleteByNotificationIds(notificationsToDelete);
                log.info("Deleted {} obsolete payment notifications for user {}", notificationsToDelete.size(), userId);
            } else {
                log.info("No obsolete payment notifications found for user {}", userId);
            }
            
            return notificationsToDelete.size();
            
        } catch (Exception e) {
            log.error("Error cleaning up obsolete payment notifications for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Extract payment ID from notification message
     */
    private Integer extractPaymentIdFromMessage(String message) {
        if (message == null) return null;
        
        try {
            // Pattern to match "Hóa đơn #123" or "hóa đơn #123"
            Pattern pattern = Pattern.compile("[Hh]óa đơn #(\\d+)");
            Matcher matcher = pattern.matcher(message);
            
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.warn("Error extracting payment ID from message: {}", message, e);
        }
        
        return null;
    }

    /**
     * Delete specific payment notifications for a payment ID
     */
    public void deletePaymentNotifications(Integer userId, Integer paymentId) {
        try {
            List<Notification> notifications = notificationRepository.findPaymentNotificationsByUserAndPaymentId(
                    userId, Notification.NotificationType.PAYMENT, paymentId);
            
            if (!notifications.isEmpty()) {
                List<Integer> notificationIds = notifications.stream()
                        .map(Notification::getNotificationId)
                        .toList();
                
                notificationRepository.deleteByNotificationIds(notificationIds);
                log.info("Deleted {} payment notifications for user {} and payment {}", 
                        notificationIds.size(), userId, paymentId);
            }
        } catch (Exception e) {
            log.error("Error deleting payment notifications for user {} and payment {}: {}", 
                    userId, paymentId, e.getMessage(), e);
        }
    }
}
