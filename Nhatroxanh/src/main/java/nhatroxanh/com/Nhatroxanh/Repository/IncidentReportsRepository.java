package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;

public interface IncidentReportsRepository extends JpaRepository<IncidentReports, Integer> {
    @Query("SELECT r FROM IncidentReports r " +
            "JOIN r.user u " +
            "JOIN r.room rm " +
            "JOIN rm.hostel h " +
            "WHERE LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY r.reportedAt DESC")
    Page<IncidentReports> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM IncidentReports r ORDER BY r.reportedAt DESC")
    Page<IncidentReports> findAllPaged(Pageable pageable);

    List<IncidentReports> findByRoom(Rooms room);

    @Query("SELECT i FROM IncidentReports i WHERE i.room.hostel.owner.userId = :hostId")
    Page<IncidentReports> findByHostId(Integer hostId, Pageable pageable);

    @Query("SELECT i FROM IncidentReports i WHERE i.room.hostel.owner.userId = :hostId " +
           "AND (LOWER(i.incidentType) LIKE :search OR LOWER(i.user.fullname) LIKE :search " +
           "OR LOWER(i.room.hostel.name) LIKE :search)")
    Page<IncidentReports> findByHostIdAndSearch(Integer hostId, String search, Pageable pageable);

}
