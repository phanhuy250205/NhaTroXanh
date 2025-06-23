
package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contracts, Integer> {
    
    @Query("SELECT c FROM Contracts c WHERE c.room.roomId = :roomId")
    List<Contracts> findByRoomId(@Param("roomId") Integer roomId);
    
    List<Contracts> findByTenantUserId(@Param("userId") Integer userId);
    
    List<Contracts> findByStatus(Contracts.Status status);
    
    @Query("SELECT c FROM Contracts c WHERE c.owner.userId = :ownerId")
    List<Contracts> findByOwnerId(@Param("ownerId") Integer ownerId);
    
    @Query("SELECT COUNT(c) FROM Contracts c WHERE c.owner.userId = :ownerId")
    Long countByOwnerId(@Param("ownerId") Integer ownerId);
    
    @Query("SELECT COUNT(c) FROM Contracts c WHERE c.owner.userId = :ownerId AND c.status = :status")
    Long countByOwnerIdAndStatus(@Param("ownerId") Integer ownerId, @Param("status") Contracts.Status status);
    
    @Query("SELECT SUM(c.price) FROM Contracts c WHERE c.owner.userId = :ownerId AND c.status = :status")
    Float getTotalRevenueByOwnerId(@Param("ownerId") Integer ownerId, @Param("status") Contracts.Status status);
    
    @Query("SELECT c FROM Contracts c WHERE c.room.roomId = :roomId AND c.status = :status")
    Optional<Contracts> findActiveContractByRoomId(@Param("roomId") Integer roomId, @Param("status") Contracts.Status status);
    
    @Query("SELECT c FROM Contracts c WHERE c.endDate <= :endDate AND c.status = :status")
    List<Contracts> findByEndDateLessThanEqualAndStatus(@Param("endDate") Date endDate, @Param("status") Contracts.Status status);
    
    @Query("SELECT c FROM Contracts c WHERE c.startDate >= :startDate AND c.endDate <= :endDate")
    List<Contracts> findByDateRange(@Param("startDate") java.sql.Date startDate, 
                                   @Param("endDate") java.sql.Date endDate);

    @Query("SELECT c FROM Contracts c WHERE c.tenant.fullname LIKE %:name%")
    List<Contracts> findByTenantName(@Param("name") String name);
    
    @Query("SELECT c FROM Contracts c WHERE c.tenantPhone = :phone")
    List<Contracts> findByTenantPhone(@Param("phone") String phone);
    
    @Query("SELECT c FROM Contracts c WHERE c.tenant.cccd = :cccd")
    List<Contracts> findByTenantCccd(@Param("cccd") String cccd);

    @Query("SELECT c FROM Contracts c WHERE c.room.roomId = :roomId AND c.status = :status")
    List<Contracts> findByRoomIdAndStatus(@Param("roomId") Integer roomId, @Param("status") Contracts.Status status);
}
