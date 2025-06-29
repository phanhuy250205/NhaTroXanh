package nhatroxanh.com.Nhatroxanh.Repository;

// LỖI 1: Xóa import sai
// import org.apache.hc.core5.annotation.Contract; 
// import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;

// SỬA: Thêm các import ĐÚNG
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // <-- Pageable đúng là ở đây
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts; // <-- Import lớp Entity Contracts của bạn

@Repository
// LỖI 2: Sửa "Contract" thành "Contracts" cho đúng với tên lớp Entity của bạn
public interface ContractsRepository extends JpaRepository<Contracts, Integer> { 
    
     @Query("SELECT c FROM Contracts c JOIN c.user u " +
           // Thêm LEFT JOIN để không bị lỗi nếu khách chưa có thông tin CCCD
           "LEFT JOIN u.userCccd ucccd " + 
           "JOIN c.room r JOIN r.hostel h WHERE h.owner.userId = :ownerId " +
           "AND (:hostelId IS NULL OR h.hostelId = :hostelId) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR u.phone LIKE CONCAT('%', :keyword, '%') " +
           // Tìm kiếm trên bảng user_cccd
           "OR ucccd.cccdNumber LIKE CONCAT('%', :keyword, '%'))")
    Page<Contracts> findTenantsByOwnerWithFilters(
        @Param("ownerId") Integer ownerId,
        @Param("keyword") String keyword,
        @Param("hostelId") Integer hostelId,
        @Param("status") Boolean status,
        Pageable pageable 
    );
    
}