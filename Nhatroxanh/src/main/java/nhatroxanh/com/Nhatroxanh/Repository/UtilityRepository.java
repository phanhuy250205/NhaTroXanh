package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;

public interface UtilityRepository extends JpaRepository<Utility, Integer> {
    @Query("SELECT u FROM Utility u ORDER BY u.name ASC")
    List<Utility> findAll();

    @Query("SELECT DISTINCT u FROM Utility u " +
            "JOIN u.posts p " +
            "WHERE p.status = true " +
            "AND p.approvalStatus = 'APPROVED' " +
            "ORDER BY u.name ASC")
    List<Utility> findUtilitiesWithActivePosts();
}
