package nhatroxanh.com.Nhatroxanh.Repository;

import nhatroxanh.com.Nhatroxanh.Model.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // ✅ Method của bạn - OK
    List<ChatMessage> findByHostelIdOrderByTimestampAsc(Long hostelId);

    // ✅ Method của bạn - OK nhưng có thể cải tiến
    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.hostelId = :hostelId AND c.senderId != :userId AND c.isRead = false")
    Long countUnreadMessages(@Param("hostelId") Long hostelId, @Param("userId") Long userId);

    // ✅ Method của bạn - OK
    @Query("SELECT c FROM ChatMessage c WHERE c.hostelId = :hostelId ORDER BY c.timestamp DESC")
    List<ChatMessage> findRecentMessagesByHostel(@Param("hostelId") Long hostelId);

    // 🔥 CÁC METHOD BỔ SUNG QUAN TRỌNG:

    // Pagination cho chat history
    List<ChatMessage> findByHostelIdOrderByTimestampAsc(Long hostelId, Pageable pageable);

    List<ChatMessage> findByHostelIdOrderByTimestampDesc(Long hostelId, Pageable pageable);

    // Tìm tin nhắn theo chat room
    List<ChatMessage> findByChatRoomOrderByTimestampAsc(String chatRoom, Pageable pageable);

    // Tìm tin nhắn chưa đọc của một user cụ thể
    @Query("SELECT c FROM ChatMessage c WHERE c.hostelId = :hostelId AND c.receiverId = :receiverId AND c.isRead = false ORDER BY c.timestamp ASC")
    List<ChatMessage> findUnreadMessagesByReceiver(@Param("hostelId") Long hostelId, @Param("receiverId") Long receiverId);

    // Đếm tin nhắn chưa đọc theo receiverId (chính xác hơn)
    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.hostelId = :hostelId AND c.receiverId = :receiverId AND c.isRead = false")
    Long countUnreadMessagesByReceiver(@Param("hostelId") Long hostelId, @Param("receiverId") Long receiverId);

    // Tìm conversation giữa 2 users
    @Query("SELECT c FROM ChatMessage c WHERE " +
            "((c.senderId = :user1 AND c.receiverId = :user2) OR " +
            "(c.senderId = :user2 AND c.receiverId = :user1)) AND " +
            "c.hostelId = :hostelId ORDER BY c.timestamp ASC")
    List<ChatMessage> findConversationBetweenUsers(
            @Param("user1") Long user1,
            @Param("user2") Long user2,
            @Param("hostelId") Long hostelId,
            Pageable pageable);

    // Đánh dấu tin nhắn đã đọc
    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage c SET c.isRead = true WHERE " +
            "c.hostelId = :hostelId AND c.receiverId = :receiverId AND c.isRead = false")
    int markMessagesAsRead(@Param("hostelId") Long hostelId, @Param("receiverId") Long receiverId);

    // Đánh dấu một tin nhắn cụ thể đã đọc
    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage c SET c.isRead = true WHERE c.messageId = :messageId")
    int markMessageAsRead(@Param("messageId") Long messageId);

    // Tìm tin nhắn theo type
    List<ChatMessage> findByHostelIdAndTypeOrderByTimestampDesc(Long hostelId, ChatMessage.MessageType type);

    // Tìm tin nhắn support
    @Query("SELECT c FROM ChatMessage c WHERE c.hostelId = :hostelId AND c.isSupportMessage = true ORDER BY c.timestamp DESC")
    List<ChatMessage> findSupportMessagesByHostel(@Param("hostelId") Long hostelId);

    // Tìm tin nhắn trong khoảng thời gian
    @Query("SELECT c FROM ChatMessage c WHERE c.hostelId = :hostelId AND c.timestamp BETWEEN :startTime AND :endTime ORDER BY c.timestamp ASC")
    List<ChatMessage> findMessagesByTimeRange(
            @Param("hostelId") Long hostelId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Xóa tin nhắn cũ (cho cleanup job)
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage c WHERE c.timestamp < :cutoffDate")
    void deleteOldMessages(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Tìm tin nhắn mới nhất của mỗi hostel
    @Query("SELECT c FROM ChatMessage c WHERE c.messageId IN " +
            "(SELECT MAX(c2.messageId) FROM ChatMessage c2 GROUP BY c2.hostelId)")
    List<ChatMessage> findLatestMessagePerHostel();

    // Tìm tin nhắn theo session (để track user activity)
    List<ChatMessage> findBySessionIdOrderByTimestampDesc(String sessionId);

    // Đếm tổng tin nhắn của hostel
    Long countByHostelId(Long hostelId);

    // Tìm tin nhắn theo priority
    List<ChatMessage> findByHostelIdAndPriorityOrderByTimestampDesc(Long hostelId, ChatMessage.MessagePriority priority);

    // Tìm tin nhắn theo status
    List<ChatMessage> findByHostelIdAndStatusOrderByTimestampDesc(Long hostelId, ChatMessage.MessageStatus status);

    @Query("SELECT c FROM ChatMessage c WHERE c.hostelId = :hostelId AND c.content LIKE %:keyword% ORDER BY c.timestamp DESC")
    List<ChatMessage> findByHostelIdAndContentContainingIgnoreCaseOrderByTimestampDesc(
            @Param("hostelId") Long hostelId,
            @Param("keyword") String keyword);

    Long countByHostelIdAndIsReadFalse(Long hostelId);

    List<ChatMessage> findByHostelIdOrderByTimestampDesc(Long hostelId);
}
