package nhatroxanh.com.Nhatroxanh.Repository;

import java.sql.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;

@Repository
public interface PaymentsRepository extends JpaRepository<Payments, Integer> {

    // Tính tổng doanh thu trong khoảng thời gian
    @Query("SELECT SUM(p.totalAmount) FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId " +
           "AND p.paymentStatus = 'ĐÃ_THANH_TOÁN' AND p.paymentDate BETWEEN :startDate AND :endDate")
    Float sumRevenueByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);

    // Đếm số phòng đang nợ
    @Query("SELECT COUNT(DISTINCT p.contract.room) FROM Payments p WHERE p.contract.room.hostel.owner.userId = :ownerId " +
           "AND p.paymentStatus = 'CHƯA_THANH_TOÁN'")
    long countOverdueRoomsByOwnerId(Integer ownerId);

    // Lấy dữ liệu doanh thu theo ngày cho biểu đồ (dùng cho lọc tháng)
    @Query("SELECT FUNCTION('DATE', p.paymentDate) AS paymentDay, SUM(p.totalAmount) FROM Payments p " +
           "WHERE p.contract.room.hostel.owner.userId = :ownerId AND p.paymentStatus = 'ĐÃ_THANH_TOÁN' " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE', p.paymentDate)")
    List<Object[]> getDailyRevenueByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);

    // Lấy dữ liệu doanh thu theo tháng cho biểu đồ (dùng cho lọc quý)
    @Query("SELECT FUNCTION('MONTH', p.paymentDate) AS paymentMonth, SUM(p.totalAmount) FROM Payments p " +
           "WHERE p.contract.room.hostel.owner.userId = :ownerId AND p.paymentStatus = 'ĐÃ_THANH_TOÁN' " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('MONTH', p.paymentDate)")
    List<Object[]> getMonthlyRevenueByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);

    // Lấy dữ liệu doanh thu theo quý cho biểu đồ (dùng cho lọc năm)
    @Query("SELECT FUNCTION('QUARTER', p.paymentDate) AS paymentQuarter, SUM(p.totalAmount) FROM Payments p " +
           "WHERE p.contract.room.hostel.owner.userId = :ownerId AND p.paymentStatus = 'ĐÃ_THANH_TOÁN' " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('QUARTER', p.paymentDate)")
    List<Object[]> getQuarterlyRevenueByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);
}
