package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import nhatroxanh.com.Nhatroxanh.Model.enity.Notification;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Repository.NotificationRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                log.info("Unauthenticated access to /api/notifications, returning empty response");
                return ResponseEntity.ok(Map.of(
                        "notifications", Collections.emptyList(),
                        "unreadCount", 0
                ));
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();
            log.info("Fetching notifications for user ID: {}", userId);

            List<Notification> notifications = notificationRepository.findByUserUserIdOrderByCreateAtDesc(userId);
            List<Map<String, Object>> enrichedNotifications = notifications.stream()
                    .filter(Objects::nonNull)
                    .map(notification -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("notificationId", notification.getNotificationId());
                        map.put("title", notification.getTitle());
                        map.put("message", notification.getMessage());
                        map.put("type", notification.getType().toString());
                        map.put("isRead", notification.getIsRead());
                        map.put("createAt", notification.getCreateAt());

                        // Include room data with detailed logging
                        if (notification.getRoom() != null) {
                            Rooms room = notification.getRoom();
                            Map<String, Object> roomMap = new HashMap<>();
                            roomMap.put("roomId", room.getRoomId());
                            roomMap.put("namerooms", room.getNamerooms());
                            roomMap.put("acreage", room.getAcreage());
                            roomMap.put("price", room.getPrice());
                            if (room.getCategory() != null) {
                                roomMap.put("category", Map.of("name", room.getCategory().getName()));
                            }
                            map.put("room", roomMap);
                            log.debug("Added room data for notification {}: {}", notification.getNotificationId(), roomMap);
                        } else {
                            log.warn("No room data for notification {} (room_id is null or not loaded), room_id from DB: {}", 
                                    notification.getNotificationId(), notification.getRoom() != null ? notification.getRoom().getRoomId() : "null");
                        }

                        // Parse payment details for PAYMENT notifications
                        if (notification.getType() == Notification.NotificationType.PAYMENT) {
                            Map<String, Object> paymentDetails = parsePaymentNotification(notification.getMessage());
                            if (paymentDetails != null && paymentDetails.get("invoiceId") != null) {
                                map.put("paymentId", paymentDetails.get("invoiceId"));
                                map.put("paymentDetails", paymentDetails);
                                log.debug("Added payment details for notification {}: {}", notification.getNotificationId(), paymentDetails);
                            } else {
                                log.warn("Failed to parse payment details for notification ID: {}, message: {}", 
                                        notification.getNotificationId(), notification.getMessage());
                            }
                        }
                        return map;
                    })
                    .collect(Collectors.toList());

            long unreadCount = notificationRepository.countByUserUserIdAndIsReadFalse(userId);
            log.info("Found {} notifications ({} unread) for user ID: {}", notifications.size(), unreadCount, userId);

            return ResponseEntity.ok(Map.of(
                    "notifications", enrichedNotifications,
                    "unreadCount", unreadCount));
        } catch (Exception e) {
            log.error("Error fetching notifications: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch notifications: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable Integer id, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                log.warn("Unauthenticated attempt to mark notification ID: {} as read", id);
                return ResponseEntity.status(403).body("Unauthorized access to notification");
            }

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

    @GetMapping("/view")
    public String viewNotifications(Authentication authentication, Model model) {
        try {
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                log.warn("Unauthenticated access to notifications view");
                model.addAttribute("error", "Vui lòng đăng nhập để xem thông báo");
                model.addAttribute("notifications", Collections.emptyList());
                model.addAttribute("unreadCount", 0);
                return "guest/chitiet-thongbao";
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();
            log.info("Rendering notifications view for user ID: {}", userId);

            List<Notification> notifications = notificationRepository.findByUserUserIdOrderByCreateAtDesc(userId);
            if (notifications == null) {
                notifications = Collections.emptyList();
                log.warn("No notifications found for user ID: {}", userId);
            }

            long unreadCount = notificationRepository.countByUserUserIdAndIsReadFalse(userId);
            log.info("Found {} notifications ({} unread) for user ID: {}", notifications.size(), unreadCount, userId);

            List<Map<String, Object>> enrichedNotifications = notifications.stream()
                    .filter(Objects::nonNull)
                    .map(notification -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("notification", notification);

                        if (notification.getRoom() != null) {
                            Rooms room = notification.getRoom();
                            Map<String, Object> roomMap = new HashMap<>();
                            roomMap.put("roomId", room.getRoomId());
                            roomMap.put("namerooms", room.getNamerooms());
                            roomMap.put("acreage", room.getAcreage());
                            roomMap.put("price", room.getPrice());
                            if (room.getCategory() != null) {
                                roomMap.put("category", Map.of("name", room.getCategory().getName()));
                            }
                            map.put("room", roomMap);
                        }

                        if (notification.getType() == Notification.NotificationType.PAYMENT) {
                            Map<String, Object> paymentDetails = parsePaymentNotification(notification.getMessage());
                            if (paymentDetails != null) {
                                map.put("paymentDetails", paymentDetails);
                            }
                        }
                        return map;
                    })
                    .collect(Collectors.toList());

            model.addAttribute("notifications", enrichedNotifications);
            model.addAttribute("unreadCount", unreadCount);
            return "guest/chitiet-thongbao";
        } catch (Exception e) {
            log.error("Error rendering notifications view: ", e);
            model.addAttribute("error", "Không thể tải thông báo: " + e.getMessage());
            model.addAttribute("notifications", Collections.emptyList());
            model.addAttribute("unreadCount", 0);
            return "guest/chitiet-thongbao";
        }
    }

    private Map<String, Object> parsePaymentNotification(String message) {
        try {
            Map<String, Object> paymentDetails = new HashMap<>();
            
            // Check if this is a success notification
            if (message.contains("thanh toán thành công") || message.contains("Bạn đã thanh toán thành công")) {
                // Parse success notification
                Pattern successPattern = Pattern.compile("Bạn đã thanh toán thành công hóa đơn #(\\d+) cho phòng ([^\\s]+) tại ([^.]+)\\. Tháng: ([\\d/]+)\\. Số tiền: ([^.]+)\\. Phương thức: ([^.]+)\\.");
                Matcher successMatcher = successPattern.matcher(message);
                
                if (successMatcher.find()) {
                    paymentDetails.put("invoiceId", successMatcher.group(1));
                    paymentDetails.put("month", successMatcher.group(4));
                    paymentDetails.put("total", successMatcher.group(5));
                    paymentDetails.put("roomName", successMatcher.group(2));
                    paymentDetails.put("hostelName", successMatcher.group(3));
                    paymentDetails.put("paymentMethod", successMatcher.group(6));
                    paymentDetails.put("status", "SUCCESS");
                    paymentDetails.put("details", Collections.emptyList());
                    log.debug("Parsed success payment details: {}", paymentDetails);
                    return paymentDetails;
                }
                
                // Fallback for success messages that don't match the pattern
                Pattern fallbackPattern = Pattern.compile("hóa đơn #(\\d+)");
                Matcher fallbackMatcher = fallbackPattern.matcher(message);
                if (fallbackMatcher.find()) {
                    paymentDetails.put("invoiceId", fallbackMatcher.group(1));
                    paymentDetails.put("status", "SUCCESS");
                    paymentDetails.put("details", Collections.emptyList());
                    return paymentDetails;
                }
            } else {
                // Parse payment reminder notification
                Pattern pattern = Pattern.compile("Hóa đơn #(\\d+) cho tháng ([\\d/]+) \\(Tổng: ([^\\)]+)\\)\\. Hạn thanh toán: ([^\\.]+)\\.?");
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    paymentDetails.put("invoiceId", matcher.group(1));
                    paymentDetails.put("month", matcher.group(2));
                    paymentDetails.put("total", matcher.group(3));
                    paymentDetails.put("dueDate", matcher.group(4));
                    paymentDetails.put("status", "PENDING");
                    paymentDetails.put("details", Collections.emptyList());
                    log.debug("Parsed pending payment details: {}", paymentDetails);
                    return paymentDetails;
                }
            }
            
            log.warn("Could not parse PAYMENT notification format: {}", message);
            return null;
        } catch (Exception e) {
            log.error("Error parsing payment notification: {}", message, e);
            return null;
        }
    }
}