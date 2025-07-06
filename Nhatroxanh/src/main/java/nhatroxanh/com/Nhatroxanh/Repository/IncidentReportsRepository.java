package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports;

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

}
