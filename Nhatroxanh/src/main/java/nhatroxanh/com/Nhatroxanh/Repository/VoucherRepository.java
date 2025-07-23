package nhatroxanh.com.Nhatroxanh.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.VoucherStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;

public interface VoucherRepository extends JpaRepository<Vouchers, Integer> {
        // 1. Tìm tất cả voucher đang hoạt động (status = true)
        Page<Vouchers> findByStatusTrue(Pageable pageable);

        // 2. Tìm theo trạng thái cụ thể (true/false)
        Page<Vouchers> findByStatus(Boolean status, Pageable pageable);

        // 3. Tìm theo userId (người tạo voucher)
        @Query("SELECT v FROM Vouchers v WHERE v.user.userId = :userId")
        Page<Vouchers> findByUserId(@Param("userId") Integer userId, Pageable pageable);

        // 4. Tìm voucher theo mã code
        Vouchers findByCode(String code);

        // 5. Kiểm tra mã voucher có tồn tại không
        boolean existsByCode(String code);

        // 6. Tìm voucher theo hostelId (đang active)
        @Query("SELECT v FROM Vouchers v WHERE v.hostel.id = :hostelId AND v.status = true")
        List<Vouchers> findActiveVouchersByHostelId(@Param("hostelId") Integer hostelId);

        // 8. Tìm các voucher đã hết hạn
        @Query("SELECT v FROM Vouchers v WHERE v.endDate < CURRENT_DATE")
        List<Vouchers> findExpiredVouchers();

        @Query("SELECT v FROM Vouchers v WHERE v.user = :user AND " +
                        "(LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Vouchers> searchVouchers(@Param("user") Users user,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        // Tìm kiếm theo từ khóa, user và trạng thái
        @Query("SELECT v FROM Vouchers v WHERE v.user = :user AND v.status = :status AND " +
                        "(LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Vouchers> searchVouchersByStatus(@Param("user") Users user,
                        @Param("keyword") String keyword,
                        @Param("status") Boolean status,
                        Pageable pageable);

        List<Vouchers> findByUserUserId(Integer userId);

        @Query("SELECT v FROM Vouchers v WHERE v.user.userId = :userId " +
                        "AND (:searchQuery IS NULL OR LOWER(v.title) LIKE :searchQuery OR LOWER(v.code) LIKE :searchQuery) "
                        +
                        "AND (:status IS NULL OR v.status = :status)")
        Page<Vouchers> findByUserUserIdWithFilters(
                        @Param("userId") Integer userId,
                        @Param("searchQuery") String searchQuery,
                        @Param("status") Boolean status,
                        Pageable pageable);

        @Query("SELECT COUNT(v) > 0 FROM Vouchers v WHERE LOWER(TRIM(v.code)) = LOWER(TRIM(:code)) AND v.id != :id")
        boolean existsByCodeAndNotId(@Param("code") String code, @Param("id") Integer id);

        List<Vouchers> findByStatus(boolean status);

        Page<Vouchers> findByUserAndStatus(Users user, Boolean status, Pageable pageable);

        Page<Vouchers> findByUser(Users user, Pageable pageable);

}
