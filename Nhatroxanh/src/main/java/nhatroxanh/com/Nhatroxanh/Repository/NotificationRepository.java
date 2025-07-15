package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import nhatroxanh.com.Nhatroxanh.Model.enity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.room WHERE n.user.userId = :userId ORDER BY n.createAt DESC")
    List<Notification> findByUserUserIdOrderByCreateAtDesc(Integer userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.room WHERE n.user.userId = :userId AND n.isRead = false ORDER BY n.createAt DESC")
    List<Notification> findByUserUserIdAndIsReadFalseOrderByCreateAtDesc(Integer userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.room WHERE n.user.userId = :userId AND n.type = :type ORDER BY n.createAt DESC")
    List<Notification> findByUserUserIdAndTypeOrderByCreateAtDesc(@Param("userId") Integer userId, @Param("type") Notification.NotificationType type);

    long countByUserUserIdAndIsReadFalse(Integer userId);
}
