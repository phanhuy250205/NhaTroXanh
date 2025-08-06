package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import nhatroxanh.com.Nhatroxanh.Model.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    // Original methods for backward compatibility
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.room WHERE n.user.userId = :userId ORDER BY n.createAt DESC")
    List<Notification> findByUserUserIdOrderByCreateAtDesc(Integer userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.room WHERE n.user.userId = :userId AND n.isRead = false ORDER BY n.createAt DESC")
    List<Notification> findByUserUserIdAndIsReadFalseOrderByCreateAtDesc(Integer userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.room WHERE n.user.userId = :userId AND n.type = :type ORDER BY n.createAt DESC")
    List<Notification> findByUserUserIdAndTypeOrderByCreateAtDesc(@Param("userId") Integer userId, @Param("type") Notification.NotificationType type);

    // New paginated methods
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.room WHERE n.user.userId = :userId ORDER BY n.createAt DESC")
    Page<Notification> findByUserUserIdOrderByCreateAtDesc(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.room WHERE n.user.userId = :userId AND n.isRead = false ORDER BY n.createAt DESC")
    Page<Notification> findByUserUserIdAndIsReadFalseOrderByCreateAtDesc(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.room WHERE n.user.userId = :userId AND n.type = :type ORDER BY n.createAt DESC")
    Page<Notification> findByUserUserIdAndTypeOrderByCreateAtDesc(@Param("userId") Integer userId, @Param("type") Notification.NotificationType type, Pageable pageable);

    // Count methods for pagination
    long countByUserUserIdAndIsReadFalse(Integer userId);
    
    long countByUserUserId(Integer userId);
    
    long countByUserUserIdAndType(Integer userId, Notification.NotificationType type);
    
    // Find payment notifications by user and payment ID pattern
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.type = :type AND n.message LIKE CONCAT('%#', :paymentId, '%')")
    List<Notification> findPaymentNotificationsByUserAndPaymentId(@Param("userId") Integer userId, 
                                                                  @Param("type") Notification.NotificationType type, 
                                                                  @Param("paymentId") Integer paymentId);
    
    // Delete notifications by IDs
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.notificationId IN :notificationIds")
    void deleteByNotificationIds(@Param("notificationIds") List<Integer> notificationIds);
    
    // Find all payment notifications for a user
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.type = 'PAYMENT'")
    List<Notification> findAllPaymentNotificationsByUserId(@Param("userId") Integer userId);
}
