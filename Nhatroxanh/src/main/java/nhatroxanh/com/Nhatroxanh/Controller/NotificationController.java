package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import nhatroxanh.com.Nhatroxanh.Model.enity.Notification;
import nhatroxanh.com.Nhatroxanh.Repository.NotificationRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotifications(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();
            log.info("Fetching notifications for user ID: {}", userId);

            List<Notification> notifications = notificationRepository.findByUserUserIdOrderByCreateAtDesc(userId);
            long unreadCount = notifications.stream().filter(n -> !n.getIsRead()).count();
            log.info("Found {} notifications ({} unread) for user ID: {}", notifications.size(), unreadCount, userId);

            return ResponseEntity.ok(Map.of(
                "notifications", notifications,
                "unreadCount", unreadCount
            ));
        } catch (Exception e) {
            log.error("Error fetching notifications: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch notifications: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable Integer id, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();
            log.info("Marking notification ID: {} as read for user ID: {}", id, userId);

            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));

            if (!notification.getUser().getUserId().equals(userId)) {
                log.warn("Unauthorized attempt to mark notification ID: {} by user ID: {}", id, userId);
                return ResponseEntity.status(403).body("Unauthorized access to notification");
            }

            notification.setIsRead(true);
            notificationRepository.save(notification);
            log.info("Marked notification ID: {} as read", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking notification ID: {} as read: ", id, e);
            return ResponseEntity.badRequest().body("Error marking notification as read: " + e.getMessage());
        }
    }
}