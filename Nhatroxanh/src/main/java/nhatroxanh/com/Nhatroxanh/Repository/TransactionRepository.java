package nhatroxanh.com.Nhatroxanh.Repository;


import nhatroxanh.com.Nhatroxanh.Model.entity.Transaction;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.entity.Transaction;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    // Tìm giao dịch theo user
    List<Transaction> findByUserOrderByCreatedAtDesc(Users user);
    
    Page<Transaction> findByUserOrderByCreatedAtDesc(Users user, Pageable pageable);

    // Tìm giao dịch theo user và loại giao dịch
    List<Transaction> findByUserAndTransactionTypeOrderByCreatedAtDesc(Users user, Transaction.TransactionType transactionType);
    
    Page<Transaction> findByUserAndTransactionTypeOrderByCreatedAtDesc(Users user, Transaction.TransactionType transactionType, Pageable pageable);

    // Tìm giao dịch theo trạng thái
    List<Transaction> findByStatusOrderByCreatedAtDesc(Transaction.TransactionStatus status);
    
    Page<Transaction> findByStatusOrderByCreatedAtDesc(Transaction.TransactionStatus status, Pageable pageable);

    // Tìm giao dịch theo user và trạng thái
    List<Transaction> findByUserAndStatusOrderByCreatedAtDesc(Users user, Transaction.TransactionStatus status);
    
    Page<Transaction> findByUserAndStatusOrderByCreatedAtDesc(Users user, Transaction.TransactionStatus status, Pageable pageable);

    // Tìm giao dịch chờ duyệt
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactions();
    
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.approvedBy WHERE t.status = 'PENDING' ORDER BY t.createdAt ASC")
    Page<Transaction> findPendingTransactions(Pageable pageable);

    // Tìm giao dịch chờ duyệt theo loại
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.transactionType = :type ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactionsByType(@Param("type") Transaction.TransactionType type);
    
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.approvedBy WHERE t.status = 'PENDING' AND t.transactionType = :type ORDER BY t.createdAt ASC")
    Page<Transaction> findPendingTransactionsByType(@Param("type") Transaction.TransactionType type, Pageable pageable);

    // Thống kê giao dịch theo user
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user = :user AND t.status = :status")
    Long countByUserAndStatus(@Param("user") Users user, @Param("status") Transaction.TransactionStatus status);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.transactionType = :type AND t.status = 'COMPLETED'")
    Double sumAmountByUserAndTypeAndCompleted(@Param("user") Users user, @Param("type") Transaction.TransactionType type);

    // Tìm giao dịch trong khoảng thời gian
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByUserAndDateRange(@Param("user") Users user, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Tìm giao dịch theo reference
    List<Transaction> findByTransactionReference(String transactionReference);

    // Đếm giao dịch chờ duyệt
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'PENDING'")
    Long countPendingTransactions();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'PENDING' AND t.transactionType = :type")
    Long countPendingTransactionsByType(@Param("type") Transaction.TransactionType type);

    // Tìm giao dịch được duyệt bởi staff
    List<Transaction> findByApprovedByOrderByProcessedAtDesc(Users approvedBy);
    
    Page<Transaction> findByApprovedByOrderByProcessedAtDesc(Users approvedBy, Pageable pageable);

    // Tìm kiếm giao dịch
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.approvedBy WHERE " +
           "(:userId IS NULL OR t.user.userId = :userId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:type IS NULL OR t.transactionType = :type) AND " +
           "(:keyword IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.user.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.accountHolderName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> searchTransactions(@Param("userId") Integer userId,
                                       @Param("status") Transaction.TransactionStatus status,
                                       @Param("type") Transaction.TransactionType type,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);
}
