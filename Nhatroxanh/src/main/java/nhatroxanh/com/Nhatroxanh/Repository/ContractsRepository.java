package nhatroxanh.com.Nhatroxanh.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

import org.apache.hc.core5.annotation.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.sql.Date;

import java.util.List;

@Repository
public interface ContractsRepository extends JpaRepository<Contracts, Integer> {

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
                        "WHERE c.room.hostel.owner.userId = :ownerId AND c.status = 'ACTIVE' " +
                        "AND c.endDate BETWEEN :currentDate AND :futureDate")
        long countExpiringContractsByOwnerId(Integer ownerId, Date currentDate, Date futureDate);

        @Query("SELECT c FROM Contracts c WHERE c.tenant.userCccd.cccdNumber = :cccd")
        List<Contract> findByTenantCccd(@Param("cccd") String cccd);

         Optional<Contracts> findTopByTenantOrderByStartDateDesc(Users tenant);
}