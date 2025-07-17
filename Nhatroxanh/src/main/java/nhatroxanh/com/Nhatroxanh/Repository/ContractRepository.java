package nhatroxanh.com.Nhatroxanh.Repository;

import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import org.apache.hc.core5.annotation.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contracts, Integer> {

        @Query("SELECT c FROM Contracts c JOIN FETCH c.owner JOIN FETCH c.tenant WHERE c.owner.userId = :ownerId")
        List<Contracts> findByOwnerId(@Param("ownerId") Integer ownerId);

        @Query("SELECT c FROM Contracts c WHERE c.room.roomId = :roomId")
        List<Contracts> findByRoomId(@Param("roomId") Integer roomId);

        List<Contracts> findByTenantUserId(@Param("userId") Integer userId);

        List<Contracts> findByStatus(Contracts.Status status);

        @Query("SELECT COUNT(c) FROM Contracts c WHERE c.owner.userId = :ownerId")
        Long countByOwnerId(@Param("ownerId") Integer ownerId);

        @Query("SELECT COUNT(c) FROM Contracts c WHERE c.owner.userId = :ownerId AND c.status = :status")
        Long countByOwnerIdAndStatus(@Param("ownerId") Integer ownerId, @Param("status") Contracts.Status status);

        @Query("SELECT SUM(c.price) FROM Contracts c WHERE c.owner.userId = :ownerId AND c.status = :status")
        Float getTotalRevenueByOwnerId(@Param("ownerId") Integer ownerId, @Param("status") Contracts.Status status);

        @Query("SELECT c FROM Contracts c WHERE c.room.roomId = :roomId AND c.status = :status")
        Optional<Contracts> findActiveContractByRoomId(@Param("roomId") Integer roomId,
                        @Param("status") Contracts.Status status);

        @Query("SELECT c FROM Contracts c WHERE c.endDate <= :endDate AND c.status = :status")
        List<Contracts> findByEndDateLessThanEqualAndStatus(@Param("endDate") Date endDate,
                        @Param("status") Contracts.Status status);

        @Query("SELECT c FROM Contracts c WHERE c.startDate >= :startDate AND c.endDate <= :endDate")
        List<Contracts> findByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

        // SỬA LẠI: Thay fullName thành fullname (chữ thường)
        @Query("SELECT c FROM Contracts c WHERE c.tenant.fullname LIKE %:name%")
        List<Contracts> findByTenantName(@Param("name") String name);

        @Query("SELECT c FROM Contracts c WHERE c.tenantPhone = :phone")
        List<Contracts> findByTenantPhone(@Param("phone") String phone);

        @Query("SELECT c FROM Contracts c WHERE c.tenant.userCccd.cccdNumber = :cccd")
        List<Contracts> findByTenantCccd(@Param("cccd") String cccd);

        @Query("SELECT c FROM Contracts c WHERE c.room.roomId = :roomId AND c.status = :status")
        List<Contracts> findByRoomIdAndStatus(@Param("roomId") Integer roomId,
                        @Param("status") Contracts.Status status);

        @Query("SELECT c FROM Contracts c WHERE c.unregisteredTenant.id = :unregisteredTenantId")
        List<Contracts> findByUnregisteredTenantId(@Param("unregisteredTenantId") Integer unregisteredTenantId);

        @Query("SELECT c FROM Contracts c ORDER BY c.contractDate DESC")
        List<Contracts> findAllOrderByContractDateDesc();

        @Query("SELECT c FROM Contracts c " +
                        "JOIN c.room r " +
                        "JOIN r.hostel h " +
                        "WHERE h.owner.userId = :ownerId " +
                        "ORDER BY c.contractDate DESC")
        List<Contracts> findByOwnerUserIdOrderByContractDateDesc(@Param("ownerId") Integer ownerId);

        List<Contracts> findByTenantAndStatusIn(Users tenant, List<Contracts.Status> statuses);

        List<Contracts> findByTenant(Users tenant);

        Page<Contracts> findByTenant(Users tenant, Pageable pageable);

        @Query("SELECT c.status, COUNT(c) FROM Contracts c WHERE c.room.hostel.owner.userId = :ownerId GROUP BY c.status")
        List<Object[]> countContractsByStatus(@Param("ownerId") Integer ownerId);

        // Thay thế method updateContract bằng @Query
        @Modifying
        @Transactional
        @Query("UPDATE Contracts c SET " +
                        "c.contractDate = :#{#contractDto.contractDate}, " +
                        "c.startDate = :#{#contractDto.terms.startDate}, " +
                        "c.endDate = :#{#contractDto.terms.endDate}, " +
                        "c.price = :#{#contractDto.terms.price}, " +
                        "c.deposit = :#{#contractDto.terms.deposit}, " +
                        "c.status = :#{#contractDto.status} " +
                        "WHERE c.contractId = :contractId")
        int updateContract(
                        @Param("contractId") Integer contractId,
                        @Param("contractDto") ContractDto contractDto);

        // Hoặc nếu muốn trả về đối tượng Contracts
        @Query("SELECT c FROM Contracts c WHERE c.contractId = :contractId")
        Optional<Contracts> findByContractId(@Param("contractId") Integer contractId);

        // Tìm hợp đồng theo tenant ID
        @Query("SELECT c FROM Contracts c WHERE c.tenant.userId = :tenantId OR c.unregisteredTenant.id = :tenantId")
        Optional<Contracts> findByTenantId(@Param("tenantId") Long tenantId);

}