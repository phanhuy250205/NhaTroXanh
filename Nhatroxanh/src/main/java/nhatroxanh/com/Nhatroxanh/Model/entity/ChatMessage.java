package nhatroxanh.com.Nhatroxanh.Model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_hostel_timestamp", columnList = "hostelId, timestamp"),
        @Index(name = "idx_sender_timestamp", columnList = "senderId, timestamp"),
        @Index(name = "idx_chat_room", columnList = "chatRoom"),
        @Index(name = "idx_unread", columnList = "isRead, hostelId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 100)
    private String senderName;

    @Column(nullable = false)
    private Long hostelId;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Column(name = "chat_room", length = 100)
    private String chatRoom;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type = MessageType.CHAT;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_support_message", nullable = false)
    private Boolean isSupportMessage = false;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessagePriority priority = MessagePriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.SENT;

    public enum MessageType {
        CHAT, JOIN, LEAVE, SYSTEM, TYPING, FILE, IMAGE, NOTIFICATION
    }

    public enum MessagePriority {
        LOW, NORMAL, HIGH, URGENT
    }

    public enum MessageStatus {
        SENT, DELIVERED, READ, FAILED
    }

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (chatRoom == null && hostelId != null) {
            chatRoom = "hostel_" + hostelId;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
