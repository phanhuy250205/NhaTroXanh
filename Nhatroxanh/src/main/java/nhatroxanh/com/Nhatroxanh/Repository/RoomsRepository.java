package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.RoomStatus;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.Utility;

public interface RoomsRepository extends JpaRepository<Rooms, Integer> {

        // New method to fetch Room with Hostel, Address, Ward, District, and Province
        @Query("SELECT r FROM Rooms r " +
                        "LEFT JOIN FETCH r.hostel h " +
                        "LEFT JOIN FETCH h.address a " +
                        "LEFT JOIN FETCH a.ward w " +
                        "LEFT JOIN FETCH w.district d " +
                        "LEFT JOIN FETCH d.province p " +
                        "WHERE r.roomId = :roomId")
        Optional<Rooms> findByIdWithFullAddress(@Param("roomId") Integer roomId);

        // Existing methods
        @Query("SELECT r FROM Rooms r " +
                        "LEFT JOIN FETCH r.hostel h " +
                        "LEFT JOIN FETCH h.owner " +
                        "LEFT JOIN FETCH r.category " +
                        "LEFT JOIN FETCH r.utilities " +
                        "WHERE r.roomId = :roomId")
        Optional<Rooms> findByIdWithUtilities(@Param("roomId") Integer roomId);

        @Query("SELECT r FROM Rooms r " +
                        "LEFT JOIN FETCH r.hostel " +
                        "LEFT JOIN FETCH r.category " +
                        "WHERE r.roomId = :roomId")
        Optional<Rooms> findByIdWithDetails(@Param("roomId") Integer roomId);

        @Query("SELECT r FROM Rooms r " +
                        "LEFT JOIN FETCH r.hostel " +
                        "LEFT JOIN FETCH r.category")
        List<Rooms> findAllWithDetails();

        @Query("SELECT u FROM Utility u JOIN u.rooms r WHERE r.roomId = :roomId")
        Set<Utility> findUtilitiesByRoomId(@Param("roomId") Integer roomId);

        @Query("SELECT r FROM Rooms r " +
                        "LEFT JOIN FETCH r.hostel h " +
                        "LEFT JOIN FETCH h.owner " +
                        "LEFT JOIN FETCH r.category " +
                        "WHERE h.owner.userId = :userId")
        List<Rooms> findByUserId(@Param("userId") Integer userId);

        @Query("SELECT r FROM Rooms r " +
                        "LEFT JOIN FETCH r.category " +
                        "WHERE r.hostel.hostelId = :hostelId")
        List<Rooms> findByHostelId(@Param("hostelId") Integer hostelId);

        @Query("SELECT r FROM Rooms r " +
                        "LEFT JOIN FETCH r.hostel " +
                        "WHERE r.category.categoryId = :categoryId")
        List<Rooms> findByCategoryId(@Param("categoryId") Integer categoryId);

        @Query("SELECT r FROM Rooms r " +
                        "LEFT JOIN FETCH r.hostel " +
                        "LEFT JOIN FETCH r.category " +
                        "WHERE r.status = 'AVAILABLE'")
        List<Rooms> findAvailableRooms();

        @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId")
        long countRoomsByOwnerId(Integer ownerId);

        @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId AND r.status = 'unactive'")
        long countVacantRoomsByOwnerId(Integer ownerId);

        @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId AND r.status = 'active'")
        long countRentedRoomsByOwnerId(Integer ownerId);

        @Query("SELECT DISTINCT h FROM Hostel h " +
                        "LEFT JOIN FETCH h.rooms r " +
                        "LEFT JOIN FETCH r.category " +
                        "LEFT JOIN FETCH r.utilities " +
                        "LEFT JOIN FETCH r.images " +
                        "WHERE h.hostelId = :hostelId")
        Optional<Hostel> findByIdWithRooms(@Param("hostelId") Integer hostelId);

        @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId AND r.status = 'ĐANG_CỌC'")
        long countDepositedRoomsByOwnerId(Integer ownerId);

        @Query("SELECT r FROM Rooms r WHERE r.namerooms = :roomNumber")
        Optional<Rooms> findByRoomNumber(@Param("roomNumber") String roomNumber);

        // Truy vấn để lấy phòng trọ theo hostelId
        @Query("SELECT r FROM Rooms r WHERE r.hostel.hostelId = :hostelId")
        List<Rooms> findRoomsByHostelId(@Param("hostelId") Integer hostelId);

        // Truy vấn với JOIN FETCH để lấy phòng trọ cùng thông tin khu trọ và tiện ích
        @Query("SELECT r FROM Rooms r LEFT JOIN FETCH r.hostel h LEFT JOIN FETCH r.utilities u WHERE r.hostel.hostelId = :hostelId")
        List<Rooms> findRoomsWithDetailsByHostelId(@Param("hostelId") Integer hostelId);

        Optional<Rooms> findFirstByHostel(Hostel hostel);

        Long countByStatus(RoomStatus status);

        List<Rooms> findByHostel_HostelId(Integer hostelId);

        @Modifying
        @Query(value = "DELETE FROM room_utility WHERE room_id = ?1", nativeQuery = true)
        void deleteRoomUtilitiesByRoomId(Integer roomId);
}
