package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;

public interface HostelRepository extends JpaRepository<Hostel, Integer> {
    @Query("SELECT h FROM Hostel h LEFT JOIN FETCH h.owner")
    List<Hostel> findAllWithDetails();

    @Query("SELECT h FROM Hostel h WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Hostel> findByNameContainingIgnoreCase(String name);

}
