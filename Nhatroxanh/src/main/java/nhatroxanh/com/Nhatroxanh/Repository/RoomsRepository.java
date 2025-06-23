package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;

public interface RoomsRepository extends JpaRepository<Rooms, Integer> {
    
    @Query("SELECT r FROM Rooms r LEFT JOIN FETCH r.hostel h LEFT JOIN FETCH h.owner LEFT JOIN FETCH r.category LEFT JOIN FETCH r.utilities WHERE r.roomId = :roomId")
    Optional<Rooms> findByIdWithUtilities(@Param("roomId") Integer roomId);
    
    @Query("SELECT r FROM Rooms r LEFT JOIN FETCH r.hostel LEFT JOIN FETCH r.category WHERE r.roomId = :roomId")
    Optional<Rooms> findByIdWithDetails(@Param("roomId") Integer roomId);
    
    @Query("SELECT r FROM Rooms r LEFT JOIN FETCH r.hostel LEFT JOIN FETCH r.category")
    List<Rooms> findAllWithDetails();
}