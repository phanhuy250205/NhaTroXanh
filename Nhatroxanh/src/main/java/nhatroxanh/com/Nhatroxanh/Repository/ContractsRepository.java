package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.entity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractsRepository extends JpaRepository<Contracts, Integer> {

        // ✅ THÊM JOIN FETCH ĐỂ TRÁNH LAZY LOADING
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "LEFT JOIN FETCH c.owner o " +
                "WHERE c.owner.userId = :ownerId " +
                "AND (:keyword IS NULL OR t.fullname LIKE %:keyword% OR c.tenantPhone LIKE %:keyword%) " +
                "AND (:hostelId IS NULL OR r.hostel.hostelId = :hostelId) " +
                "AND (:status IS NULL OR c.status = :status)")
        Page<Contracts> findTenantsByOwnerWithFilters(
                @Param("ownerId") Integer ownerId,
                @Param("keyword") String keyword,
                @Param("hostelId") Integer hostelId,
                @Param("status") Contracts.Status status,
                Pageable pageable);

        // ✅ THÊM JOIN FETCH
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "LEFT JOIN FETCH c.owner o " +
                "WHERE c.owner.userId = :ownerId")
        List<Contracts> findByOwnerId(@Param("ownerId") Integer ownerId);

        // Đếm tổng số người thuê hiện tại (hợp đồng đang hoạt động)
        @Query("SELECT COUNT(DISTINCT c.tenant.userId) FROM Contracts c " +
                "WHERE c.room.hostel.owner.userId = :ownerId AND c.status = 'ACTIVE' " +
                "AND c.endDate >= :currentDate")
        long countActiveTenantsByOwnerId(Integer ownerId, Date currentDate);

        // Đếm số người thuê mới trong khoảng thời gian
        @Query("SELECT COUNT(DISTINCT c.tenant.userId) FROM Contracts c " +
                "WHERE c.room.hostel.owner.userId = :ownerId " +
                "AND c.startDate BETWEEN :startDate AND :endDate")
        long countNewTenantsByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);

        // Đếm số hợp đồng sắp hết hạn trong 30 ngày tới
        @Query("SELECT COUNT(c) FROM Contracts c " +
                "WHERE c.room.hostel.owner.userId = :ownerId AND c.status = 'EXPIRED' " +
                "AND c.endDate BETWEEN :currentDate AND :futureDate")
        long countExpiringContractsByOwnerId(Integer ownerId, Date currentDate, Date futureDate);

        // ✅ THÊM JOIN FETCH
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "WHERE (t.userCccd.cccdNumber = :cccd OR ut.cccdNumber = :cccd)")
        List<Contracts> findByTenantCccd(@Param("cccd") String cccd);

        List<Contracts> findByTenantOrderByStartDateDesc(Users tenant);

        Long countByStatus(Contracts.Status status);

        @Query("SELECT c FROM Contracts c " +
                "JOIN FETCH c.room r " +
                "JOIN FETCH r.hostel h " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "WHERE h.owner.userId = :ownerId AND c.returnReason IS NOT NULL " +
                "ORDER BY c.createdAt DESC")
        Page<Contracts> findReturnRequestsByOwner(@Param("ownerId") Integer ownerId, Pageable pageable);

        @Query("SELECT c FROM Contracts c " +
                "JOIN FETCH c.room r " +
                "JOIN FETCH r.hostel h " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "WHERE h.owner.userId = :ownerId AND c.returnReason IS NOT NULL " +
                "AND (LOWER(t.fullname) LIKE %:keyword% OR LOWER(r.namerooms) LIKE %:keyword%) " +
                "ORDER BY c.createdAt DESC")
        Page<Contracts> findReturnRequestsByOwnerAndKeyword(@Param("ownerId") Integer ownerId,
                                                            @Param("keyword") String keyword, Pageable pageable);

        // Đếm số hợp đồng theo trạng thái và chủ trọ
        Long countByOwnerUserIdAndStatus(Integer ownerId, Contracts.Status status);

        // ✅ SỬA LẠI VỚI JOIN FETCH
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "LEFT JOIN FETCH c.owner o " +
                "WHERE c.tenantPhone LIKE %:phone% AND c.owner.userId = :ownerId")
        List<Contracts> findByTenantPhoneAndOwnerUserId(@Param("phone") String phone, @Param("ownerId") Integer ownerId);

        // ✅ SỬA LẠI VỚI JOIN FETCH
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "WHERE c.tenant.userId IN :userIds AND c.owner.userId = :ownerId")
        List<Contracts> findByTenantUserIdInAndOwnerUserId(@Param("userIds") List<Integer> userIds, @Param("ownerId") Integer ownerId);

        // ✅ SỬA LẠI VỚI JOIN FETCH
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "WHERE c.unregisteredTenant.id IN :unregisteredTenantIds AND c.owner.userId = :ownerId")
        List<Contracts> findByUnregisteredTenantIdInAndOwnerUserId(@Param("unregisteredTenantIds") List<Integer> unregisteredTenantIds, @Param("ownerId") Integer ownerId);

        Long countByOwnerUserIdAndEndDateBetweenAndStatus(Integer ownerId, Date startDate, Date endDate, Contracts.Status status);

        List<Contracts> findByStatusAndEndDateLessThanEqual(Contracts.Status status, Date endDate);

        // ✅ SỬA LẠI VỚI JOIN FETCH
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "LEFT JOIN FETCH c.owner o " +
                "WHERE LOWER(r.namerooms) LIKE LOWER(CONCAT('%', :roomName, '%')) AND c.owner.userId = :ownerId")
        List<Contracts> findByRoomNameAndOwnerUserId(@Param("roomName") String roomName, @Param("ownerId") Integer ownerId);

        // ✅ PHIÊN BẢN PHÂN TRANG VỚI JOIN FETCH CHO SEARCH BY PHONE
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "LEFT JOIN FETCH c.owner o " +
                "WHERE c.tenantPhone LIKE %:phone% AND c.owner.userId = :ownerId")
        Page<Contracts> findByTenantPhoneAndOwnerUserId(@Param("phone") String phone, @Param("ownerId") Integer ownerId, Pageable pageable);

        // ✅ PHIÊN BẢN PHÂN TRANG VỚI JOIN FETCH CHO SEARCH BY ROOM NAME
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "LEFT JOIN FETCH c.owner o " +
                "WHERE LOWER(r.namerooms) LIKE :roomName AND c.owner.userId = :ownerId")
        Page<Contracts> findByRoomNameAndOwnerUserId(@Param("roomName") String roomName, @Param("ownerId") Integer ownerId, Pageable pageable);

        // ✅ PHIÊN BẢN PHÂN TRANG VỚI JOIN FETCH CHO LIST ALL
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "LEFT JOIN FETCH c.owner o " +
                "WHERE c.owner.userId = :ownerId")
        Page<Contracts> findByOwnerUserId(@Param("ownerId") Integer ownerId, Pageable pageable);

        // ✅ THÊM METHOD MỚI CHO SEARCH TỔNG HỢP
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "LEFT JOIN FETCH c.owner o " +
                "WHERE c.owner.userId = :ownerId " +
                "AND (:phone IS NULL OR c.tenantPhone LIKE %:phone%) " +
                "AND (:roomName IS NULL OR LOWER(r.namerooms) LIKE LOWER(CONCAT('%', :roomName, '%')))")
        Page<Contracts> findByOwnerWithSearchCriteria(
                @Param("ownerId") Integer ownerId,
                @Param("phone") String phone,
                @Param("roomName") String roomName,
                Pageable pageable);

        // ✅ METHOD MỚI VỚI JOIN FETCH RÕ RÀNG
        @Query("SELECT c FROM Contracts c " +
                "LEFT JOIN FETCH c.room r " +
                "LEFT JOIN FETCH c.tenant t " +
                "LEFT JOIN FETCH c.unregisteredTenant ut " +
                "WHERE c.owner.userId = :ownerId")
        Page<Contracts> findByOwnerUserIdWithRoom(@Param("ownerId") Integer ownerId, Pageable pageable);
}
