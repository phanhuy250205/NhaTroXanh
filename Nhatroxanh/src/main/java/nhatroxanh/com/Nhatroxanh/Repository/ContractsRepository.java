package nhatroxanh.com.Nhatroxanh.Repository;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Date;

@Repository
public interface ContractsRepository extends JpaRepository<Contracts, Integer> {

    // Đếm tổng số người thuê hiện tại (hợp đồng đang hoạt động)
    @Query("SELECT COUNT(DISTINCT c.user.userId) FROM Contracts c " +
           "WHERE c.room.hostel.owner.userId = :ownerId AND c.status = true " +
           "AND c.endDate >= :currentDate")
    long countActiveTenantsByOwnerId(Integer ownerId, Date currentDate);

    // Đếm số người thuê mới trong khoảng thời gian
    @Query("SELECT COUNT(DISTINCT c.user.userId) FROM Contracts c " +
           "WHERE c.room.hostel.owner.userId = :ownerId " +
           "AND c.startDate BETWEEN :startDate AND :endDate")
    long countNewTenantsByOwnerIdAndDateRange(Integer ownerId, Date startDate, Date endDate);

    // Đếm số hợp đồng sắp hết hạn trong 30 ngày tới
    @Query("SELECT COUNT(c) FROM Contracts c " +
           "WHERE c.room.hostel.owner.userId = :ownerId AND c.status = true " +
           "AND c.endDate BETWEEN :currentDate AND :futureDate")
    long countExpiringContractsByOwnerId(Integer ownerId, Date currentDate, Date futureDate);
}