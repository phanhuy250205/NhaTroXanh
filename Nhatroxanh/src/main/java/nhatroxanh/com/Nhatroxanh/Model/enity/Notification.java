package nhatroxanh.com.Nhatroxanh.Model.enity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "Notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // Prevent circular reference
    private Users user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private NotificationType type;

    @Column(nullable = false)
    private Boolean isRead;

    @Column(name = "create_at", nullable = false)
    private Date createAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
    @JsonIgnore
    private Rooms room;

    public enum NotificationType {
        PAYMENT, CONTRACT, SYSTEM, REPORT
    }
}