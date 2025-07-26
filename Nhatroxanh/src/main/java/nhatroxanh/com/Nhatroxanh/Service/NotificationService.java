package nhatroxanh.com.Nhatroxanh.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.entity.Notification;
import nhatroxanh.com.Nhatroxanh.Model.entity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
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

    public Notification createIncidentNotification(Users user, IncidentReports incident, String title, String message) {
        Rooms room = null;
        if (incident != null && incident.getRoom() != null) {
            room = incident.getRoom();
        }

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(Notification.NotificationType.REPORT)
                .isRead(false)
                .createAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
                .room(room)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Created incident notification for user {} and incident {}: {}", 
                user.getUserId(), incident.getReportId(), message);
        return savedNotification;
    }

    public void handlePaymentSuccess(Users user, Payments payment, String successTitle, String successMessage) {
        try {
            if (user == null || payment == null) {
                log.error("Cannot handle payment success: user or payment is null");
                return;
            }

            List<Notification> existingNotifications = notificationRepository.findByUserUserIdAndTypeOrderByCreateAtDesc(
                    user.getUserId(), Notification.NotificationType.PAYMENT);
            
            String paymentIdStr = String.valueOf(payment.getId());
            boolean notificationUpdated = false;
            
            for (Notification notification : existingNotifications) {
                if (notification.getMessage() != null && notification.getMessage().contains("Hóa đơn #" + paymentIdStr)) {
                    if (!notification.getTitle().contains("thành công")) {
                        String updatedMessage = updateMessageToSuccess(notification.getMessage(), payment);
                        notification.setTitle(successTitle != null ? successTitle : "Thanh toán thành công");
                        notification.setMessage(updatedMessage);
                        notification.setIsRead(false);
                        notificationRepository.save(notification);
                        log.info("Updated existing payment notification {} to success status", notification.getNotificationId());
                        notificationUpdated = true;
                        break;
                    }
                }
            }
            
            if (!notificationUpdated) {
                createPaymentNotification(user, payment, successTitle, successMessage);
                log.info("Created new payment success notification for user {} and payment {}", user.getUserId(), payment.getId());
            }
        } catch (Exception e) {
            log.error("Error handling payment success notifications for user {} and payment {}: {}", 
                    user.getUserId(), payment.getId(), e.getMessage(), e);
        }
    }

    private String updateMessageToSuccess(String originalMessage, Payments payment) {
        try {
            if (payment == null) {
                log.warn("Payment is null, using fallback message");
                return "Thanh toán thành công! Cảm ơn bạn đã thanh toán đúng hạn.";
            }

            String invoiceId = String.valueOf(payment.getId());
            String roomName = "N/A";
            String hostelName = "N/A";
            String paymentMethod = "N/A";
            String formattedAmount = "N/A";

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

            if (payment.getPaymentMethod() != null) {
                switch (payment.getPaymentMethod()) {
                    case VNPAY:
                        paymentMethod = "VNPay";
                        break;
                    case MOMO:
                        paymentMethod = "MoMo";
                        break;
                    case ZALOPAY:
                        paymentMethod = "ZaloPay";
                        break;
                    default:
                        paymentMethod = payment.getPaymentMethod().toString();
                }
            }

            if (payment.getTotalAmount() != null) {
                DecimalFormat formatter = new DecimalFormat("#,###");
                formattedAmount = formatter.format(payment.getTotalAmount()) + " VNĐ";
            }

            String month = "N/A";
            if (originalMessage != null) {
                Pattern pattern = Pattern.compile("cho tháng ([\\d/]+)");
                Matcher matcher = pattern.matcher(originalMessage);
                if (matcher.find()) {
                    month = matcher.group(1);
                }
            }

            return String.format(
                "Bạn đã thanh toán thành công hóa đơn #%s cho phòng %s tại %s. " +
                "Tháng: %s. Số tiền: %s. Phương thức: %s. " +
                "Cảm ơn bạn đã thanh toán đúng hạn!",
                invoiceId, roomName, hostelName, month, formattedAmount, paymentMethod
            );
        } catch (Exception e) {
            log.error("Error updating message to success: {}", e.getMessage(), e);
            if (payment != null && payment.getId() != null) {
                return String.format("Thanh toán thành công hóa đơn #%d! Cảm ơn bạn đã thanh toán đúng hạn.", payment.getId());
            }
            return "Thanh toán thành công! Cảm ơn bạn đã thanh toán đúng hạn.";
        }
    }

    public int cleanupObsoletePaymentNotifications(Integer userId) {
        try {
            log.info("Starting cleanup of obsolete payment notifications for user {}", userId);
            List<Notification> paymentNotifications = notificationRepository.findAllPaymentNotificationsByUserId(userId);
            List<Integer> notificationsToDelete = new ArrayList<>();
            
            for (Notification notification : paymentNotifications) {
                Integer paymentId = extractPaymentIdFromMessage(notification.getMessage());
                
                if (paymentId != null) {
                    if (!paymentsRepository.existsByPaymentId(paymentId)) {
                        notificationsToDelete.add(notification.getNotificationId());
                        log.debug("Marking notification {} for deletion - payment {} no longer exists", 
                                notification.getNotificationId(), paymentId);
                    } else {
                        Optional<Payments.PaymentStatus> statusOpt = paymentsRepository.findPaymentStatusById(paymentId);
                        if (statusOpt.isPresent() && statusOpt.get() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
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

    private Integer extractPaymentIdFromMessage(String message) {
        if (message == null) return null;
        
        try {
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

    /**
     * Create account-related notification for password changes
     */
    public Notification createPasswordChangeNotification(Users user) {
        try {
            String title = "Mật khẩu đã được thay đổi";
            String message = String.format(
                "Mật khẩu tài khoản của bạn đã được thay đổi thành công vào lúc %s. " +
                "Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ với chúng tôi ngay lập tức để bảo mật tài khoản.",
                java.time.LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .type(Notification.NotificationType.ACCOUNT)
                    .isRead(false)
                    .createAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
                    .room(null)
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            log.info("Created password change notification for user {}: {}", user.getUserId(), message);
            return savedNotification;
        } catch (Exception e) {
            log.error("Error creating password change notification for user {}: {}", user.getUserId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create account-related notification for profile updates
     */
    public Notification createProfileUpdateNotification(Users user, String updatedFields) {
        try {
            String title = "Thông tin tài khoản đã được cập nhật";
            String message = String.format(
                "Thông tin tài khoản của bạn đã được cập nhật thành công vào lúc %s. " +
                "Các thông tin đã thay đổi: %s. " +
                "Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ với chúng tôi ngay lập tức.",
                java.time.LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                updatedFields
            );

            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .type(Notification.NotificationType.ACCOUNT)
                    .isRead(false)
                    .createAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
                    .room(null)
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            log.info("Created profile update notification for user {}: {}", user.getUserId(), message);
            return savedNotification;
        } catch (Exception e) {
            log.error("Error creating profile update notification for user {}: {}", user.getUserId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create hostel activity notification
     */
    public Notification createHostelActivityNotification(Users user, String title, String message, Rooms room) {
        try {
            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .type(Notification.NotificationType.HOSTEL_ACTIVITY)
                    .isRead(false)
                    .createAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
                    .room(room)
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            log.info("Created hostel activity notification for user {} and room {}: {}", 
                    user.getUserId(), room != null ? room.getRoomId() : "null", message);
            return savedNotification;
        } catch (Exception e) {
            log.error("Error creating hostel activity notification for user {}: {}", user.getUserId(), e.getMessage(), e);
            return null;
        }
    }
}