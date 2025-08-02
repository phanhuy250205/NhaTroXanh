package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.Notification;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.NotificationRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomOAuth2UserDetails;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.NumberFormat;
import java.text.ParseException;
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
    private final NotificationService notificationService;

    /**
     * Hàm helper để lấy thông tin Users một cách an toàn từ bất kỳ loại đăng nhập nào.
     */
    private Users getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUser();
        } else if (principal instanceof CustomOAuth2UserDetails) {
            return ((CustomOAuth2UserDetails) principal).getUser();
        }
        return null;
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotifications(Authentication authentication) {
        try {
            Users user = getUserFromAuthentication(authentication);
            if (user == null) {
                log.info("Unauthenticated access to /api/notifications, returning empty response");
                return ResponseEntity.ok(Map.of("notifications", Collections.emptyList(), "unreadCount", 0));
            }

            Integer userId = user.getUserId();
            log.info("Fetching notifications for user ID: {}", userId);

            try {
                int cleanedUp = notificationService.cleanupObsoletePaymentNotifications(userId);
                if (cleanedUp > 0) {
                    log.info("Cleaned up {} obsolete payment notifications for user {}", cleanedUp, userId);
                }
            } catch (Exception e) {
                log.warn("Failed to cleanup obsolete notifications for user {}: {}", userId, e.getMessage());
            }

            List<Notification> notifications = notificationRepository.findByUserUserIdOrderByCreateAtDesc(userId);
            List<Map<String, Object>> enrichedNotifications = notifications.stream()
                    .filter(Objects::nonNull)
                    .map(this::enrichNotification)
                    .collect(Collectors.toList());

            long unreadCount = notificationRepository.countByUserUserIdAndIsReadFalse(userId);
            log.info("Found {} notifications ({} unread) for user ID: {}", notifications.size(), unreadCount, userId);

            return ResponseEntity.ok(Map.of("notifications", enrichedNotifications, "unreadCount", unreadCount));
        } catch (Exception e) {
            log.error("Error fetching notifications: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch notifications: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable Integer id, Authentication authentication) {
        try {
            Users user = getUserFromAuthentication(authentication);
            if (user == null) {
                log.warn("Unauthenticated attempt to mark notification ID: {} as read", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to notification");
            }

            Integer userId = user.getUserId();
            log.info("Marking notification ID: {} as read for user ID: {}", id, userId);

            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));

            if (!notification.getUser().getUserId().equals(userId)) {
                log.warn("Unauthorized attempt to mark notification ID: {} by user ID: {}", id, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to notification");
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
            Users user = getUserFromAuthentication(authentication);
            if (user == null) {
                log.warn("Unauthenticated access to notifications view");
                model.addAttribute("error", "Vui lòng đăng nhập để xem thông báo");
                model.addAttribute("notifications", Collections.emptyList());
                model.addAttribute("unreadCount", 0);
                return "guest/chitiet-thongbao";
            }
            
            Integer userId = user.getUserId();
            log.info("Rendering notifications view for user ID: {}", userId);

            try {
                int cleanedUp = notificationService.cleanupObsoletePaymentNotifications(userId);
                if (cleanedUp > 0) {
                    log.info("Cleaned up {} obsolete payment notifications for user {}", cleanedUp, userId);
                }
            } catch (Exception e) {
                log.warn("Failed to cleanup obsolete notifications for user {}: {}", userId, e.getMessage());
            }

            List<Notification> notifications = notificationRepository.findByUserUserIdOrderByCreateAtDesc(userId);
            long unreadCount = notificationRepository.countByUserUserIdAndIsReadFalse(userId);
            
            List<Map<String, Object>> enrichedNotifications = notifications.stream()
                .filter(Objects::nonNull)
                .map(this::enrichNotification)
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
    
    @PostMapping("/cleanup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cleanupObsoleteNotifications(Authentication authentication) {
        try {
            Users user = getUserFromAuthentication(authentication);
            if (user == null) {
                log.warn("Unauthenticated attempt to cleanup notifications");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Unauthorized access"));
            }

            Integer userId = user.getUserId();
            log.info("Manual cleanup request for user ID: {}", userId);

            int cleanedUp = notificationService.cleanupObsoletePaymentNotifications(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cleanup completed successfully",
                    "cleanedUp", cleanedUp));
        } catch (Exception e) {
            log.error("Error during manual cleanup: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to cleanup notifications: " + e.getMessage()));
        }
    }
    
    // =================================================================
    // CÁC HÀM HELPER ĐỂ XỬ LÝ VÀ ĐỊNH DẠNG DỮ LIỆU
    // =================================================================

    private Map<String, Object> enrichNotification(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("notificationId", notification.getNotificationId());
        map.put("title", notification.getTitle());
        map.put("message", notification.getMessage());
        map.put("type", notification.getType().toString());
        map.put("isRead", notification.getIsRead());
        map.put("createAt", notification.getCreateAt());
        map.put("notification", notification); // For viewNotifications

        if (notification.getRoom() != null) {
            Rooms room = notification.getRoom();
            Map<String, Object> roomMap = new HashMap<>();
            roomMap.put("roomId", room.getRoomId());
            roomMap.put("namerooms", room.getNamerooms());
            roomMap.put("acreage", room.getAcreage());
            roomMap.put("price", formatVietnameseCurrency(room.getPrice()));
            if (room.getCategory() != null) {
                roomMap.put("category", Map.of("name", room.getCategory().getName()));
            }
            map.put("room", roomMap);
        }

        switch (notification.getType()) {
            case PAYMENT:
                map.put("paymentDetails", parsePaymentNotification(notification.getMessage()));
                break;
            case REPORT:
                map.put("incidentDetails", parseIncidentNotification(notification.getMessage()));
                break;
            case PROMOTION:
                map.put("voucherDetails", parseVoucherNotification(notification.getMessage()));
                break;
            default:
                break;
        }
        return map;
    }

    private Map<String, Object> parsePaymentNotification(String message) {
        try {
            Map<String, Object> paymentDetails = new HashMap<>();
            if (message.contains("thanh toán thành công")) {
                Pattern p = Pattern.compile("hóa đơn #(\\d+).*tháng ([\\d/]+).*Số tiền: ([\\d.,]+)[^\\d]*\\. Phương thức: ([^.]+)\\.");
                Matcher m = p.matcher(message);
                if (m.find()) {
                    paymentDetails.put("invoiceId", m.group(1));
                    paymentDetails.put("month", m.group(2));
                    paymentDetails.put("total", formatVietnameseCurrency(parseNumber(m.group(3))));
                    paymentDetails.put("paymentMethod", m.group(4).trim());
                    paymentDetails.put("status", "SUCCESS");
                    return paymentDetails;
                }
            } else {
                Pattern p = Pattern.compile("Hóa đơn #(\\d+) cho tháng ([\\d/]+) \\(Tổng: ([\\d.,]+)[^\\)]*\\)\\. Hạn thanh toán: ([^\\.]+)\\.?");
                Matcher m = p.matcher(message);
                if (m.find()) {
                    paymentDetails.put("invoiceId", m.group(1));
                    paymentDetails.put("month", m.group(2));
                    paymentDetails.put("total", formatVietnameseCurrency(parseNumber(m.group(3))));
                    paymentDetails.put("dueDate", m.group(4).trim());
                    paymentDetails.put("status", "PENDING");
                    return paymentDetails;
                }
            }
        } catch (Exception e) {
            log.error("Error parsing payment notification: {}", message, e);
        }
        return null;
    }
    
    private Map<String, Object> parseIncidentNotification(String message) {
        try {
            Map<String, Object> details = new HashMap<>();
            Pattern p = Pattern.compile("Sự cố #(\\d+) \\(([^,]+), mức độ: ([^)]+)\\) tại phòng ([^\\s]+)");
            Matcher m = p.matcher(message);
            if (m.find()) {
                details.put("incidentId", m.group(1));
                details.put("incidentType", m.group(2));
                details.put("level", m.group(3));
                details.put("roomName", m.group(4));
                details.put("status", message.contains("đang được xử lý") ? "DANG_XU_LY" : "DA_XU_LY");
                return details;
            }
        } catch (Exception e) {
            log.error("Error parsing incident notification: {}", message, e);
        }
        return null;
    }
    
    private Map<String, Object> parseVoucherNotification(String message) {
        try {
            Map<String, Object> details = new HashMap<>();
            Pattern p = Pattern.compile("Mã voucher: ([^\\s]+)\\s*Giá trị giảm: ([\\d.,]+)\\s*VNĐ\\s*Đơn tối thiểu: ([\\d.,]+)\\s*VNĐ\\s*Hạn sử dụng: ([^\\s]+)", Pattern.DOTALL);
            Matcher m = p.matcher(message);
            if (m.find()) {
                details.put("voucherCode", m.group(1));
                details.put("discountValue", formatVietnameseCurrency(100000));
                details.put("minAmount", formatVietnameseCurrency(1000000));
                details.put("endDate", m.group(4));
                return details;
            }
        } catch (Exception e) {
            log.error("Error parsing voucher notification: {}", message, e);
        }
        return null;
    }

    private String formatVietnameseCurrency(Number amount) {
        if (amount == null) return "0 VNĐ";
        try {
            return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(Math.round(amount.doubleValue())) + " VNĐ";
        } catch (Exception e) {
            return amount + " VNĐ";
        }
    }

    private Number parseNumber(String amount) throws ParseException {
        if (amount == null || amount.trim().isEmpty()) return 0;
        String cleanAmount = amount.replaceAll("[^\\d.,]", "").trim();
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).parse(cleanAmount);
    }
}