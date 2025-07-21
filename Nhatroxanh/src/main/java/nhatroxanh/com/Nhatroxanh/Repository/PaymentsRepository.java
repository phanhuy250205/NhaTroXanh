package nhatroxanh.com.Nhatroxanh.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.entity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.entity.Payments.PaymentStatus;

@Repository
public interface PaymentsRepository extends JpaRepository<Payments, Integer> {

       // Tính tổng doanh thu trong khoảng thời gian
       @Query("SELECT SUM(p.totalAmount) FROM Payments p WHERE p.contract.owner.userId = :ownerId " +
                     "AND p.paymentStatus = 'ĐÃ_THANH_TOÁN' AND p.paymentDate BETWEEN :startDate AND :endDate")
       Float sumRevenueByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);

       // Đếm số phòng đang nợ
       @Query("SELECT COUNT(DISTINCT p.contract.room) FROM Payments p WHERE p.contract.owner.userId = :ownerId " +
                     "AND p.paymentStatus = 'CHƯA_THANH_TOÁN'")
       long countOverdueRoomsByOwnerId(Integer ownerId);

       // Lấy dữ liệu doanh thu theo ngày cho biểu đồ (dùng cho lọc tháng)
       @Query("SELECT FUNCTION('DATE', p.paymentDate), SUM(p.totalAmount) FROM Payments p " +
                     "WHERE p.contract.owner.userId = :ownerId AND p.paymentStatus = 'ĐÃ_THANH_TOÁN' " +
                     "AND p.paymentDate BETWEEN :startDate AND :endDate " +
                     "GROUP BY FUNCTION('DATE', p.paymentDate)")
       List<Object[]> getDailyRevenueByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);

       // Lấy dữ liệu doanh thu theo tháng cho biểu đồ (dùng cho lọc quý)
       @Query("SELECT FUNCTION('MONTH', p.paymentDate), SUM(p.totalAmount) FROM Payments p " +
                     "WHERE p.contract.owner.userId = :ownerId AND p.paymentStatus = 'ĐÃ_THANH_TOÁN' " +
                     "AND p.paymentDate BETWEEN :startDate AND :endDate " +
                     "GROUP BY FUNCTION('MONTH', p.paymentDate)")
       List<Object[]> getMonthlyRevenueByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);

       // Lấy dữ liệu doanh thu theo quý cho biểu đồ (dùng cho lọc năm)
       @Query("SELECT FUNCTION('QUARTER', p.paymentDate), SUM(p.totalAmount) FROM Payments p " +
                     "WHERE p.contract.owner.userId = :ownerId AND p.paymentStatus = 'ĐÃ_THANH_TOÁN' " +
                     "AND p.paymentDate BETWEEN :startDate AND :endDate " +
                     "GROUP BY FUNCTION('QUARTER', p.paymentDate)")
       List<Object[]> getQuarterlyRevenueByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);

    // === NEW QUERIES FOR PAYMENT MANAGEMENT ===
    
    // Lấy tất cả payments của owner (không phân trang - để tương thích ngược)
    @Query("SELECT p FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId ORDER BY p.dueDate DESC")
    List<Payments> findByOwnerId(@Param("ownerId") Integer ownerId);
    
    // Lấy payments của owner với phân trang
    @Query("SELECT p FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId ORDER BY p.dueDate DESC")
    Page<Payments> findByOwnerIdWithPagination(@Param("ownerId") Integer ownerId, Pageable pageable);
    
    // Lấy payments theo owner và status (không phân trang - để tương thích ngược)
    @Query("SELECT p FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId AND p.paymentStatus = :status ORDER BY p.dueDate DESC")
    List<Payments> findByOwnerIdAndStatus(@Param("ownerId") Integer ownerId, @Param("status") PaymentStatus status);
    
    // Lấy payments theo owner và status với phân trang
    @Query("SELECT p FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId AND p.paymentStatus = :status ORDER BY p.dueDate DESC")
    Page<Payments> findByOwnerIdAndStatusWithPagination(@Param("ownerId") Integer ownerId, @Param("status") PaymentStatus status, Pageable pageable);
    
    // Tìm kiếm payments theo từ khóa (không phân trang - để tương thích ngược)
    @Query("SELECT DISTINCT p FROM Payments p " +
           "LEFT JOIN p.contract c " +
           "LEFT JOIN c.room r " +
           "LEFT JOIN r.hostel h " +
           "LEFT JOIN c.tenant t " +
           "LEFT JOIN c.unregisteredTenant ut " +
           "WHERE h.owner.userId = :ownerId " +
           "AND (" +
           "LOWER(r.namerooms) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR (t IS NOT NULL AND LOWER(t.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "OR (ut IS NOT NULL AND LOWER(ut.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "OR (t IS NOT NULL AND LOWER(t.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "OR (ut IS NOT NULL AND LOWER(ut.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           ") " +
           "ORDER BY p.dueDate DESC")
    List<Payments> searchPaymentsByKeyword(@Param("ownerId") Integer ownerId, @Param("keyword") String keyword);
    
    // Tìm kiếm payments theo từ khóa với phân trang
    @Query("SELECT DISTINCT p FROM Payments p " +
           "LEFT JOIN p.contract c " +
           "LEFT JOIN c.room r " +
           "LEFT JOIN r.hostel h " +
           "LEFT JOIN c.tenant t " +
           "LEFT JOIN c.unregisteredTenant ut " +
           "WHERE h.owner.userId = :ownerId " +
           "AND (" +
           "LOWER(r.namerooms) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR (t IS NOT NULL AND LOWER(t.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "OR (ut IS NOT NULL AND LOWER(ut.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "OR (t IS NOT NULL AND LOWER(t.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "OR (ut IS NOT NULL AND LOWER(ut.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           ") " +
           "ORDER BY p.dueDate DESC")
    Page<Payments> searchPaymentsByKeywordWithPagination(@Param("ownerId") Integer ownerId, @Param("keyword") String keyword, Pageable pageable);
    
    // Lấy 8 payments mới nhất của owner
    @Query("SELECT p FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId ORDER BY p.dueDate DESC")
    List<Payments> findTop8ByOwnerIdOrderByDueDateDesc(@Param("ownerId") Integer ownerId, Pageable pageable);
    
    // Tìm payment theo contract và tháng
    @Query("SELECT p FROM Payments p WHERE p.contract.contractId = :contractId AND FUNCTION('MONTH', p.dueDate) = :month AND FUNCTION('YEAR', p.dueDate) = :year")
    Optional<Payments> findByContractIdAndMonth(@Param("contractId") Integer contractId, @Param("month") int month, @Param("year") int year);
    
    // Đếm số payments đã thanh toán của owner
    @Query("SELECT COUNT(p) FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId AND p.paymentStatus = 'ĐÃ_THANH_TOÁN'")
    long countPaidPaymentsByOwnerId(@Param("ownerId") Integer ownerId);
    
    // Đếm số payments chưa thanh toán của owner
    @Query("SELECT COUNT(p) FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId AND p.paymentStatus = 'CHƯA_THANH_TOÁN'")
    long countUnpaidPaymentsByOwnerId(@Param("ownerId") Integer ownerId);
    
    // Tính tổng doanh thu của owner
    @Query("SELECT SUM(p.totalAmount) FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId AND p.paymentStatus = 'ĐÃ_THANH_TOÁN'")
    Float getTotalRevenueByOwnerId(@Param("ownerId") Integer ownerId);

    // Đã sửa: Lấy danh sách payments theo contractId
    @Query("SELECT p FROM Payments p WHERE p.contract.contractId = :contractId ORDER BY p.dueDate DESC")
    List<Payments> findByContractId(@Param("contractId") Integer contractId);

       Optional<Payments> findByAppTransId(String appTransId);
       
    // Check if payment exists and get its status
    @Query("SELECT p.paymentStatus FROM Payments p WHERE p.id = :paymentId")
    Optional<PaymentStatus> findPaymentStatusById(@Param("paymentId") Integer paymentId);
    
    // Check if payment exists
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Payments p WHERE p.id = :paymentId")
    boolean existsByPaymentId(@Param("paymentId") Integer paymentId);
    
    // === SCHEDULER QUERIES FOR OVERDUE PAYMENTS ===
    
    // Tìm tất cả payments chưa thanh toán và đã quá hạn
    @Query("SELECT p FROM Payments p WHERE p.paymentStatus = 'CHƯA_THANH_TOÁN' AND p.dueDate < :currentDate")
    List<Payments> findUnpaidPaymentsPastDueDate(@Param("currentDate") Date currentDate);
    
    // Đếm số payments theo trạng thái
    @Query("SELECT COUNT(p) FROM Payments p WHERE p.paymentStatus = :status")
    long countByPaymentStatus(@Param("status") PaymentStatus status);
    
    // Đếm số payments mới quá hạn kể từ ngày cụ thể
    @Query("SELECT COUNT(p) FROM Payments p WHERE p.paymentStatus = 'QUÁ_HẠN_THANH_TOÁN' AND p.dueDate >= :sinceDate")
    long countNewOverduePaymentsSince(@Param("sinceDate") Date sinceDate);
}
