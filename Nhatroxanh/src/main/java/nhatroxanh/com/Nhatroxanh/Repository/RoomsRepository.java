package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;

public interface RoomsRepository extends JpaRepository<Rooms, Integer> {
    @Query("SELECT r FROM Rooms r LEFT JOIN FETCH r.hostel h LEFT JOIN FETCH h.owner LEFT JOIN FETCH r.category LEFT JOIN FETCH r.utilities WHERE r.room_id = :roomId")
    Optional<Rooms> findByIdWithUltilities(@Param("roomId") Integer roomId);

    @Query("SELECT r FROM Rooms r LEFT JOIN FETCH r.hostel LEFT JOIN FETCH r.category")
    List<Rooms> findAllWithDetails();

    @Query("SELECT r FROM Rooms r LEFT JOIN FETCH r.utilities LEFT JOIN FETCH r.images WHERE r.room_id = :roomId")
    Optional<Rooms> findByIdWithDetails(@Param("roomId") Integer roomId);

    @Query("SELECT u FROM Utility u JOIN u.rooms r WHERE r.room_id = :roomId")
    Set<Utility> findUtilitiesByRoomId(@Param("roomId") Integer roomId);
    // Đếm tổng số phòng trọ của một chủ trọ
    @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId")
    long countRoomsByOwnerId(Integer ownerId);

    // Đếm số phòng trống
    @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId AND r.status = 'unactive'")
    long countVacantRoomsByOwnerId(Integer ownerId);

    // Đếm số phòng đang thuê
    @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId AND r.status = 'active'")
    long countRentedRoomsByOwnerId(Integer ownerId);

    // Đếm số phòng đang cọc
    @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId AND r.status = 'ĐANG_CỌC'")
    long countDepositedRoomsByOwnerId(Integer ownerId);
}
