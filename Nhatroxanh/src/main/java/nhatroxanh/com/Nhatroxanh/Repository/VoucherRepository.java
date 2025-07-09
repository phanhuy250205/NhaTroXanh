package nhatroxanh.com.Nhatroxanh.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.VoucherStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;

public interface VoucherRepository extends JpaRepository<Vouchers, Integer> {
    // Tìm voucher theo VoucherStatus
    Page<Vouchers> findByVoucherStatus(VoucherStatus voucherStatus, Pageable pageable);

    // Tìm voucher theo status boolean (để tương thích với code cũ)
    Page<Vouchers> findByStatusTrue(Pageable pageable);

    // Tìm voucher theo userId
    @Query("SELECT v FROM Vouchers v WHERE v.user.userId = :userId")
    Page<Vouchers> findByUserId(@Param("userId") Integer userId, Pageable pageable);

    // Tìm voucher active theo hostelId
    @Query("SELECT v FROM Vouchers v WHERE v.hostel.id = :hostelId AND v.voucherStatus = 'APPROVED' AND v.status = true")
    List<Vouchers> findActiveVouchersByHostelId(@Param("hostelId") Integer hostelId);

    // Tìm kiếm voucher theo keyword
    @Query("SELECT v FROM Vouchers v WHERE " +
            "(LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.user.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.hostel.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Vouchers> searchVouchers(@Param("keyword") String keyword, Pageable pageable);

    // Tìm kiếm voucher theo keyword và status
    @Query("SELECT v FROM Vouchers v WHERE " +
            "v.voucherStatus = :status AND " +
            "(LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.user.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.hostel.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Vouchers> searchVouchersByStatus(@Param("keyword") String keyword,
            @Param("status") VoucherStatus status,
            Pageable pageable);

    // Kiểm tra mã voucher có tồn tại không
    boolean existsByCode(String code);

    // Tìm voucher theo code
    Vouchers findByCode(String code);

    // Đếm số voucher theo status
    @Query("SELECT COUNT(v) FROM Vouchers v WHERE v.voucherStatus = :status")
    long countByVoucherStatus(@Param("status") VoucherStatus status);

    @Query("SELECT v FROM Vouchers v WHERE v.voucherStatus = 'APPROVED' AND v.endDate < CURRENT_DATE")
    List<Vouchers> findExpiredVouchers();

    @Query("SELECT v FROM Vouchers v WHERE " +
            "(:keyword IS NULL OR v.title LIKE %:keyword% OR v.code LIKE %:keyword%) " +
            "AND (:status IS NULL OR v.status = :status) " +
            "AND (:discountType IS NULL OR v.discountType = :discountType) " +
            "AND v.voucherStatus = :voucherStatus")
    Page<Vouchers> findBySearchAndFiltersWithStatus(
            @Param("keyword") String keyword,
            @Param("status") Boolean status,
            @Param("discountType") Boolean discountType,
            @Param("voucherStatus") VoucherStatus voucherStatus,
            Pageable pageable);

    @Query("SELECT v FROM Vouchers v WHERE " +
            "(:status IS NULL OR v.status = :status) " +
            "AND (:discountType IS NULL OR v.discountType = :discountType) " +
            "AND v.voucherStatus = :voucherStatus")
    Page<Vouchers> findByFiltersWithStatus(
            @Param("status") Boolean status,
            @Param("discountType") Boolean discountType,
            @Param("voucherStatus") VoucherStatus voucherStatus,
            Pageable pageable);

}
