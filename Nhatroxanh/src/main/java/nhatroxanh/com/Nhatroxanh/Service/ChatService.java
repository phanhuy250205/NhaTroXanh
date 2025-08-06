package nhatroxanh.com.Nhatroxanh.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.ChatMessage;
import nhatroxanh.com.Nhatroxanh.Repository.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    // ✅ SAVE MESSAGE - Cải tiến với validation
    public ChatMessage saveMessage(ChatMessage message) {
        try {
            // Validation
            if (message.getHostelId() == null || message.getSenderId() == null) {
                throw new IllegalArgumentException("HostelId và SenderId không được null");
            }

            // Auto-set chat room nếu chưa có
            if (message.getChatRoom() == null) {
                message.setChatRoom("hostel_" + message.getHostelId());
            }

            // Set default values
            if (message.getType() == null) {
                message.setType(ChatMessage.MessageType.CHAT);
            }

            if (message.getIsRead() == null) {
                message.setIsRead(false);
            }

            if (message.getIsSupportMessage() == null) {
                message.setIsSupportMessage(false);
            }

            return chatMessageRepository.save(message);

        } catch (Exception e) {
            log.error("Lỗi khi lưu tin nhắn: ", e);
            throw new RuntimeException("Không thể lưu tin nhắn");
        }
    }

    // ✅ GET MESSAGES - Thêm pagination
    public List<ChatMessage> getMessagesByHostel(Long hostelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").ascending());
        return chatMessageRepository.findByHostelIdOrderByTimestampAsc(hostelId, pageable);
    }

    // Overload method không pagination (để backward compatibility)
    public List<ChatMessage> getMessagesByHostel(Long hostelId) {
        return getMessagesByHostel(hostelId, 0, 50); // Default 50 tin nhắn gần nhất
    }

    // 🔥 CHỦ TRỌ: Xem tất cả conversation với khách thuê
    public Map<Long, List<ChatMessage>> getAllConversationsForOwner(Long hostelId, Long ownerId) {
        List<ChatMessage> allMessages = chatMessageRepository.findByHostelIdOrderByTimestampDesc(hostelId);

        // Group theo senderId (khách thuê)
        return allMessages.stream()
                .filter(msg -> !msg.getSenderId().equals(ownerId)) // Loại bỏ tin nhắn của chủ trọ
                .collect(Collectors.groupingBy(ChatMessage::getSenderId));
    }

    // 🔥 KHÁCH THUÊ: Conversation với chủ trọ
    public List<ChatMessage> getConversationWithOwner(Long hostelId, Long tenantId, Long ownerId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("timestamp").descending());
        return chatMessageRepository.findConversationBetweenUsers(tenantId, ownerId, hostelId, pageable);
    }

    // ✅ UNREAD COUNT - Cải tiến cho từng role
    public Long getUnreadCountForOwner(Long hostelId, Long ownerId) {
        // Chủ trọ xem tin nhắn chưa đọc từ tất cả khách thuê
        return chatMessageRepository.countUnreadMessagesByReceiver(hostelId, ownerId);
    }

    public Long getUnreadCountForTenant(Long hostelId, Long tenantId) {
        // Khách thuê xem tin nhắn chưa đọc từ chủ trọ
        return chatMessageRepository.countUnreadMessagesByReceiver(hostelId, tenantId);
    }

    // ✅ MARK AS READ - Implementation hoàn chỉnh
    @Transactional
    public void markMessagesAsRead(Long hostelId, Long receiverId) {
        try {
            int updatedCount = chatMessageRepository.markMessagesAsRead(hostelId, receiverId);
            log.info("Đã đánh dấu {} tin nhắn là đã đọc cho user {} trong hostel {}",
                    updatedCount, receiverId, hostelId);
        } catch (Exception e) {
            log.error("Lỗi khi đánh dấu tin nhắn đã đọc: ", e);
            throw new RuntimeException("Không thể đánh dấu tin nhắn đã đọc");
        }
    }

    // 🔥 CHỦ TRỌ: Lấy danh sách khách thuê có tin nhắn mới
    public List<Long> getTenantsWithNewMessages(Long hostelId, Long ownerId) {
        return chatMessageRepository.findUnreadMessagesByReceiver(hostelId, ownerId)
                .stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .collect(Collectors.toList());
    }

    // 🔥 SUPPORT MESSAGES - Tin nhắn hỗ trợ tự động
    public ChatMessage sendSupportMessage(Long hostelId, String content, Long receiverId) {
        ChatMessage supportMessage = new ChatMessage();
        supportMessage.setHostelId(hostelId);
        supportMessage.setContent(content);
        supportMessage.setSenderName("Hỗ trợ khách hàng");
        supportMessage.setSenderId(0L); // System sender
        supportMessage.setReceiverId(receiverId);
        supportMessage.setIsSupportMessage(true);
        supportMessage.setType(ChatMessage.MessageType.SYSTEM);
        supportMessage.setPriority(ChatMessage.MessagePriority.HIGH);

        return saveMessage(supportMessage);
    }

    // 🔥 WELCOME MESSAGE - Tin nhắn chào mừng cho khách thuê mới
    public ChatMessage sendWelcomeMessage(Long hostelId, Long tenantId, String tenantName) {
        String welcomeContent = String.format(
                "Xin chào %s! Chào mừng bạn đến với nhà trọ. " +
                        "Nếu có bất kỳ thắc mắc nào, hãy nhắn tin cho chúng tôi nhé!",
                tenantName
        );

        return sendSupportMessage(hostelId, welcomeContent, tenantId);
    }

    // ✅ RECENT MESSAGES - Cải tiến với limit
    public List<ChatMessage> getRecentMessages(Long hostelId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("timestamp").descending());
        return chatMessageRepository.findByHostelIdOrderByTimestampDesc(hostelId, pageable);
    }

    public List<ChatMessage> getRecentMessages(Long hostelId) {
        return getRecentMessages(hostelId, 20); // Default 20 tin nhắn
    }

    // 🔥 CONVERSATION SUMMARY - Tóm tắt cuộc trò chuyện
    public Map<String, Object> getConversationSummary(Long hostelId, Long userId, boolean isOwner) {
        Map<String, Object> summary = new java.util.HashMap<>();

        if (isOwner) {
            // Chủ trọ
            Long unreadCount = getUnreadCountForOwner(hostelId, userId);
            List<Long> tenantsWithNewMessages = getTenantsWithNewMessages(hostelId, userId);

            summary.put("unreadCount", unreadCount);
            summary.put("tenantsWithNewMessages", tenantsWithNewMessages);
            summary.put("totalTenants", tenantsWithNewMessages.size());
        } else {
            // Khách thuê
            Long unreadCount = getUnreadCountForTenant(hostelId, userId);
            summary.put("unreadCount", unreadCount);
        }

        List<ChatMessage> recentMessages = getRecentMessages(hostelId, 5);
        summary.put("recentMessages", recentMessages);

        return summary;
    }

    // 🔥 SEARCH MESSAGES - Tìm kiếm tin nhắn
    public List<ChatMessage> searchMessages(Long hostelId, String keyword) {
        // Note: Cần thêm method này vào Repository
        return chatMessageRepository.findByHostelIdAndContentContainingIgnoreCaseOrderByTimestampDesc(
                hostelId, keyword);
    }

    // 🔥 AUTO CLEANUP - Dọn dẹp tin nhắn cũ
    @Scheduled(cron = "0 0 2 * * ?") // Chạy lúc 2h sáng hàng ngày
    public void cleanupOldMessages() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30); // Xóa tin nhắn cũ hơn 30 ngày
            chatMessageRepository.deleteOldMessages(cutoffDate);
            log.info("Đã dọn dẹp tin nhắn cũ trước ngày: {}", cutoffDate);
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp tin nhắn cũ: ", e);
        }
    }

    // 🔥 STATISTICS - Thống kê
    public Map<String, Object> getChatStatistics(Long hostelId) {
        Map<String, Object> stats = new java.util.HashMap<>();

        Long totalMessages = chatMessageRepository.countByHostelId(hostelId);
        Long unreadMessages = chatMessageRepository.countByHostelIdAndIsReadFalse(hostelId);
        List<ChatMessage> todayMessages = chatMessageRepository.findMessagesByTimeRange(
                hostelId,
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now()
        );

        stats.put("totalMessages", totalMessages);
        stats.put("unreadMessages", unreadMessages);
        stats.put("todayMessages", todayMessages.size());

        return stats;
    }
}
