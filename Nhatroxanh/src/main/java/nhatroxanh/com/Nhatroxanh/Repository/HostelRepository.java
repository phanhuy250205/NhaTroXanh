package nhatroxanh.com.Nhatroxanh.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HostelRepository extends JpaRepository<Hostel, Integer> {
    @Query("SELECT h FROM Hostel h LEFT JOIN FETCH h.address a LEFT JOIN FETCH a.ward w LEFT JOIN FETCH w.district d LEFT JOIN FETCH d.province p WHERE h.hostelId = :id")
    Optional<Hostel> findByIdWithAddress(@Param("id") Integer id);

    @Query("SELECT h FROM Hostel h LEFT JOIN FETCH h.address a LEFT JOIN FETCH a.ward w LEFT JOIN FETCH w.district d LEFT JOIN FETCH d.province p")
    List<Hostel> findAllWithDetails();

    List<Hostel> findByNameContainingIgnoreCase(String name);

    @Query("SELECT h FROM Hostel h JOIN h.owner u WHERE u.email = :email")
    Optional<Hostel> findByOwnerEmail(@Param("email") String email);

    @Query("SELECT h FROM Hostel h JOIN h.owner u WHERE u.email = :email")
    List<Hostel> findAllByOwnerEmail(@Param("email") String email);
}