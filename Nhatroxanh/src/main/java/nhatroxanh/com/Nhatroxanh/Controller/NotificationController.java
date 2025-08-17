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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
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
     * Hàm helper để lấy thông tin Users một cách an toàn từ bất kỳ loại đăng nhập
     * nào.
     */
    private Users getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
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
    public ResponseEntity<Map<String, Object>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String type) {
        try {
            Users user = getUserFromAuthentication(authentication);
            if (user == null) {
                log.info("Unauthenticated access to /api/notifications, returning empty response");
                return ResponseEntity.ok(Map.of(
                        "notifications", Collections.emptyList(),
                        "unreadCount", 0,
                        "currentPage", 0,
                        "totalPages", 0,
                        "totalItems", 0));
            }

            Integer userId = user.getUserId();
            log.info("Fetching notifications for user ID: {}, page: {}, size: {}, type: {}", userId, page, size, type);

            try {
                int cleanedUp = notificationService.cleanupObsoletePaymentNotifications(userId);
                if (cleanedUp > 0) {
                    log.info("Cleaned up {} obsolete payment notifications for user {}", cleanedUp, userId);
                }
            } catch (Exception e) {
                log.warn("Failed to cleanup obsolete notifications for user {}: {}", userId, e.getMessage());
            }

            // Use database-level pagination
            Pageable pageable = PageRequest.of(page, size);
            Page<Notification> notificationPage;

            if (type != null && !type.isEmpty()) {
                try {
                    Notification.NotificationType notificationType = Notification.NotificationType
                            .valueOf(type.toUpperCase());
                    notificationPage = notificationRepository.findByUserUserIdAndTypeOrderByCreateAtDesc(userId,
                            notificationType, pageable);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid notification type: {}", type);
                    notificationPage = notificationRepository.findByUserUserIdOrderByCreateAtDesc(userId, pageable);
                }
            } else {
                notificationPage = notificationRepository.findByUserUserIdOrderByCreateAtDesc(userId, pageable);
            }

            List<Map<String, Object>> enrichedNotifications = notificationPage.getContent().stream()
                    .filter(Objects::nonNull)
                    .map(this::enrichNotification)
                    .collect(Collectors.toList());

            long unreadCount = notificationRepository.countByUserUserIdAndIsReadFalse(userId);

            log.info("Found {} total notifications, returning {} items for page {} ({} unread) for user ID: {}",
                    notificationPage.getTotalElements(), enrichedNotifications.size(), page, unreadCount, userId);

            return ResponseEntity.ok(Map.of(
                    "notifications", enrichedNotifications,
                    "unreadCount", unreadCount,
                    "currentPage", notificationPage.getNumber(),
                    "totalPages", notificationPage.getTotalPages(),
                    "totalItems", notificationPage.getTotalElements(),
                    "hasNext", notificationPage.hasNext(),
                    "hasPrevious", notificationPage.hasPrevious()));
        } catch (Exception e) {
            log.error("Error fetching notifications: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch notifications: " + e.getMessage()));
        }
    }

    @GetMapping("/by-type")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationsByType(
            Authentication authentication,
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        try {
            Users user = getUserFromAuthentication(authentication);
            if (user == null) {
                log.info("Unauthenticated access to /api/notifications/by-type, returning empty response");
                return ResponseEntity.ok(Map.of(
                        "notifications", Collections.emptyList(),
                        "unreadCount", 0,
                        "currentPage", 0,
                        "totalPages", 0,
                        "totalItems", 0));
            }

            Integer userId = user.getUserId();
            log.info("Fetching notifications by type for user ID: {}, type: {}, page: {}, size: {}", userId, type, page,
                    size);

            try {
                Notification.NotificationType notificationType = Notification.NotificationType
                        .valueOf(type.toUpperCase());
                Pageable pageable = PageRequest.of(page, size);
                Page<Notification> notificationPage = notificationRepository
                        .findByUserUserIdAndTypeOrderByCreateAtDesc(userId, notificationType, pageable);

                List<Map<String, Object>> enrichedNotifications = notificationPage.getContent().stream()
                        .filter(Objects::nonNull)
                        .map(this::enrichNotification)
                        .collect(Collectors.toList());

                long unreadCount = notificationRepository.countByUserUserIdAndIsReadFalse(userId);

                log.info("Found {} notifications of type {} for user ID: {}, returning {} items for page {}",
                        notificationPage.getTotalElements(), type, userId, enrichedNotifications.size(), page);

                return ResponseEntity.ok(Map.of(
                        "notifications", enrichedNotifications,
                        "unreadCount", unreadCount,
                        "currentPage", notificationPage.getNumber(),
                        "totalPages", notificationPage.getTotalPages(),
                        "totalItems", notificationPage.getTotalElements(),
                        "hasNext", notificationPage.hasNext(),
                        "hasPrevious", notificationPage.hasPrevious(),
                        "type", type));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid notification type: {}", type);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid notification type: " + type));
            }
        } catch (Exception e) {
            log.error("Error fetching notifications by type: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch notifications by type: " + e.getMessage()));
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

    private Map<String, Object> parsePaymentNotification(String message) {
        try {
            log.debug("Parsing payment notification message: {}", message);
            Map<String, Object> paymentDetails = new HashMap<>();

            // Check for success notifications
            if (message.contains("thanh toán thành công") || message.contains("Bạn đã thanh toán thành công")) {
                // Primary pattern for success notifications (allow spaces in room name)
                Pattern successPattern = Pattern.compile(
                        "Bạn đã thanh toán thành công hóa đơn #(\\d+) cho phòng ([^.]+) tại ([^.]+)\\. Tháng: ([\\d/]+)\\. Số tiền: ([\\d.,]+)[^\\d]*\\. Phương thức: ([^.]+)\\.");
                Matcher successMatcher = successPattern.matcher(message);
                if (successMatcher.find()) {
                    paymentDetails.put("invoiceId", successMatcher.group(1));
                    paymentDetails.put("month", successMatcher.group(4));
                     paymentDetails.put("total",successMatcher.group(5) + " VNĐ");
                    paymentDetails.put("roomName", successMatcher.group(2).trim());
                    paymentDetails.put("hostelName", successMatcher.group(3).trim());
                    paymentDetails.put("paymentMethod", successMatcher.group(6).trim());
                    paymentDetails.put("status", "SUCCESS");
                    paymentDetails.put("details", Collections.emptyList());
                    log.debug("Parsed success payment details: {}", paymentDetails);
                    return paymentDetails;
                }

                // Alternative success pattern (more flexible)
                Pattern altSuccessPattern = Pattern.compile(
                        "thanh toán thành công.*hoá đơn #(\\d+).*tháng:?\\s*([\\d/]+).*số tiền:?\\s*([\\d.,]+).*VN[DĐ]?.*phương thức:?\\s*([^\\.\\s][^\\.]*)",
                        Pattern.CASE_INSENSITIVE);
                Matcher altSuccessMatcher = altSuccessPattern.matcher(message);
                if (altSuccessMatcher.find()) {
                    paymentDetails.put("invoiceId", altSuccessMatcher.group(1));
                    paymentDetails.put("month", altSuccessMatcher.group(2));
                    paymentDetails.put("total", parseNumber(altSuccessMatcher.group(3)));
                    paymentDetails.put("paymentMethod", altSuccessMatcher.group(4).trim());
                    paymentDetails.put("status", "SUCCESS");
                    paymentDetails.put("details", Collections.emptyList());
                    log.debug("Parsed alternative success payment details: {}", paymentDetails);
                    return paymentDetails;
                }
                // Fallback pattern for minimal success message
                Pattern fallbackPattern = Pattern.compile("hóa đơn #(\\d+)");
                Matcher fallbackMatcher = fallbackPattern.matcher(message);
                if (fallbackMatcher.find()) {
                    paymentDetails.put("invoiceId", fallbackMatcher.group(1));
                    paymentDetails.put("status", "SUCCESS");
                    paymentDetails.put("details", Collections.emptyList());

                    // Extract amount
                    Pattern amountPattern = Pattern.compile("số tiền:?\\s*([\\d.,]+)\\s*VN[DĐ]?",
                            Pattern.CASE_INSENSITIVE);
                    Matcher amountMatcher = amountPattern.matcher(message);
                    if (amountMatcher.find()) {
                        paymentDetails.put("total", parseNumber(amountMatcher.group(1)));
                    }

                    // Extract month (more flexible pattern)
                    Pattern monthPattern = Pattern.compile("tháng:?\\s*([\\d/]+(?:\\s*năm\\s*\\d+)?)",
                            Pattern.CASE_INSENSITIVE);
                    Matcher monthMatcher = monthPattern.matcher(message);
                    if (monthMatcher.find()) {
                        paymentDetails.put("month", monthMatcher.group(1).trim());
                    } else {
                        paymentDetails.put("month", "Không xác định");
                    }

                    log.debug("Parsed fallback success payment details: {}", paymentDetails);
                    return paymentDetails;
                }
            } else {
                // Pattern for pending notifications
                Pattern pattern = Pattern.compile(
                        "Hóa đơn #(\\d+) cho tháng ([\\d/]+) \\(Tổng: ([\\d.,]+)[^\\)]*\\)\\. Hạn thanh toán: ([^\\.]+)\\.?");
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    paymentDetails.put("invoiceId", matcher.group(1));
                    paymentDetails.put("month", matcher.group(2));
                    paymentDetails.put("total", parseNumber(matcher.group(3)));
                    paymentDetails.put("dueDate", matcher.group(4).trim());
                    paymentDetails.put("status", "PENDING");
                    paymentDetails.put("details", Collections.emptyList());
                    log.debug("Parsed pending payment details: {}", paymentDetails);
                    return paymentDetails;
                }

                // Alternative pending pattern
                Pattern altPendingPattern = Pattern.compile(
                        "hóa đơn #(\\d+).*tháng ([\\d/]+).*([\\d.,]+).*VN[DĐ]?.*hạn.*([\\d/]+)",
                        Pattern.CASE_INSENSITIVE);
                Matcher altPendingMatcher = altPendingPattern.matcher(message);
                if (altPendingMatcher.find()) {
                    paymentDetails.put("invoiceId", altPendingMatcher.group(1));
                    paymentDetails.put("month", altPendingMatcher.group(2));
                    paymentDetails.put("total", parseNumber(altPendingMatcher.group(3)));
                    paymentDetails.put("dueDate", altPendingMatcher.group(4).trim());
                    paymentDetails.put("status", "PENDING");
                    paymentDetails.put("details", Collections.emptyList());
                    log.debug("Parsed alternative pending payment details: {}", paymentDetails);
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

    private Number parseNumber(String amount) throws ParseException {
        if (amount == null || amount.trim().isEmpty()) {
            return 0;
        }
        String cleanAmount = amount.replaceAll("[^\\d.,]", "").trim();
        NumberFormat parser = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        return parser.parse(cleanAmount);
    }

    private Map<String, Object> parseIncidentNotification(String message) {
        try {
            Map<String, Object> incidentDetails = new HashMap<>();
            Pattern pattern = Pattern.compile("Sự cố #(\\d+) \\(([^,]+), mức độ: ([^)]+)\\) tại phòng ([^\\s]+)");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                incidentDetails.put("incidentId", matcher.group(1));
                incidentDetails.put("incidentType", matcher.group(2));
                incidentDetails.put("level", matcher.group(3));
                incidentDetails.put("roomName", matcher.group(4));
                incidentDetails.put("status", message.contains("đang được xử lý") ? "DANG_XU_LY" : "DA_XU_LY");
                log.debug("Parsed incident details: {}", incidentDetails);
                return incidentDetails;
            }
            log.warn("Could not parse INCIDENT notification format: {}", message);
            return null;
        } catch (Exception e) {
            log.error("Error parsing incident notification: {}", message, e);
            return null;
        }
    }

    private Map<String, Object> parseVoucherNotification(String message) {
        try {
            log.debug("Parsing voucher notification message: {}", message);
            Map<String, Object> voucherDetails = new HashMap<>();

            // Pattern for the new message format with line breaks
            Pattern pattern = Pattern.compile(
                    "Mã voucher: ([^\\s]+)\\s*Giá trị giảm: ([\\d.,]+)\\s*VNĐ\\s*Đơn tối thiểu: ([\\d.,]+)\\s*VNĐ\\s*Hạn sử dụng: ([^\\s]+)",
                    Pattern.DOTALL);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String voucherCode = matcher.group(1);
                String discountValueStr = matcher.group(2);
                String minAmountStr = matcher.group(3);
                String endDate = matcher.group(4);

                // Parse the numeric values
                Number discountValue = parseNumber(discountValueStr);
                Number minAmount = parseNumber(minAmountStr);
                voucherDetails.put("voucherCode", voucherCode);
                voucherDetails.put("discountValue", discountValue);
                voucherDetails.put("minAmount", minAmount);
                voucherDetails.put("endDate", endDate);
                log.debug("Parsed voucher details: {}", voucherDetails);
                return voucherDetails;
            }

            // Fallback pattern for original format
            Pattern fallbackPattern = Pattern.compile(
                    "Sử dụng mã voucher ([^\\s]+) để được giảm ([\\d.,]+) VNĐ cho đơn tối thiểu ([\\d.,]+) VNĐ\\. Hạn sử dụng đến ([^\\.]+)\\.");
            Matcher fallbackMatcher = fallbackPattern.matcher(message);
            if (fallbackMatcher.find()) {
                String voucherCode = fallbackMatcher.group(1);
                String discountValueStr = fallbackMatcher.group(2);
                String minAmountStr = fallbackMatcher.group(3);
                String endDate = fallbackMatcher.group(4);

                // Parse the numeric values
                Number discountValue = parseNumber(discountValueStr);
                Number minAmount = parseNumber(minAmountStr);
                voucherDetails.put("voucherCode", voucherCode);
                voucherDetails.put("discountValue", discountValue);
                voucherDetails.put("minAmount", minAmount);
                voucherDetails.put("endDate", endDate);
                log.debug("Parsed fallback voucher details: {}", voucherDetails);
                return voucherDetails;
            }

            log.warn("Could not parse PROMOTION notification format: {}", message);
            return null;
        } catch (Exception e) {
            log.error("Error parsing voucher notification: {}", message, e);
            return null;
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
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to cleanup notifications: " + e.getMessage()));
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

        // Format createAt as a string
        if (notification.getCreateAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String formattedDateTime = notification.getCreateAt().toLocalDateTime().format(formatter);
            map.put("createAt", formattedDateTime);
        } else {
            map.put("createAt", "N/A");
        }

        map.put("notification", notification); // For viewNotifications

        if (notification.getRoom() != null) {
            Rooms room = notification.getRoom();
            Map<String, Object> roomMap = new HashMap<>();
            roomMap.put("roomId", room.getRoomId());
            roomMap.put("namerooms", room.getNamerooms());
            roomMap.put("acreage", room.getAcreage());
            roomMap.put("price", room.getPrice());
            if (room.getHostel() != null) {
                roomMap.put("hostel", Map.of("name", room.getHostel().getName()));
                // Add hostelId for payment URL generation
                map.put("hostelId", room.getHostel().getHostelId());
            }
            map.put("room", roomMap);
            // Add roomId for payment URL generation
            map.put("roomId", room.getRoomId());
        }

        switch (notification.getType()) {
            case PAYMENT:
                Map<String, Object> paymentDetails = parsePaymentNotification(notification.getMessage());
                if (paymentDetails != null) {
                    // Add the invoiceId to the top level for easy access in JavaScript
                    map.put("invoiceId", paymentDetails.get("invoiceId"));
                    map.put("paymentDetails", paymentDetails);
                }
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
}