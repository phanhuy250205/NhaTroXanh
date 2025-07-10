package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;

public interface RoomsRepository extends JpaRepository<Rooms, Integer> {

    // Lấy room với đầy đủ thông tin và utilities
    @Query("SELECT r FROM Rooms r " +
            "LEFT JOIN FETCH r.hostel h " +
            "LEFT JOIN FETCH h.owner " +
            "LEFT JOIN FETCH r.category " +
            "LEFT JOIN FETCH r.utilities " +
            "WHERE r.roomId = :roomId")
    Optional<Rooms> findByIdWithUtilities(@Param("roomId") Integer roomId);

    // Lấy room với thông tin cơ bản (không có utilities)
    @Query("SELECT r FROM Rooms r " +
            "LEFT JOIN FETCH r.hostel " +
            "LEFT JOIN FETCH r.category " +
            "WHERE r.roomId = :roomId")
    Optional<Rooms> findByIdWithDetails(@Param("roomId") Integer roomId);

    // Lấy tất cả rooms với thông tin cơ bản
    @Query("SELECT r FROM Rooms r " +
            "LEFT JOIN FETCH r.hostel " +
            "LEFT JOIN FETCH r.category")
    List<Rooms> findAllWithDetails();

    // Lấy utilities của một room cụ thể
    @Query("SELECT u FROM Utility u JOIN u.rooms r WHERE r.roomId = :roomId")
    Set<Utility> findUtilitiesByRoomId(@Param("roomId") Integer roomId);

    // Tìm rooms theo owner ID (qua hostel)
    @Query("SELECT r FROM Rooms r " +
            "LEFT JOIN FETCH r.hostel h " +
            "LEFT JOIN FETCH h.owner " +
            "LEFT JOIN FETCH r.category " +
            "WHERE h.owner.userId = :userId")
    List<Rooms> findByUserId(@Param("userId") Integer userId);

    // Method bổ sung: Tìm rooms theo hostel
    @Query("SELECT r FROM Rooms r " +
            "LEFT JOIN FETCH r.category " +
            "WHERE r.hostel.hostelId = :hostelId")
    List<Rooms> findByHostelId(@Param("hostelId") Integer hostelId);

    // Method bổ sung: Tìm rooms theo category
    @Query("SELECT r FROM Rooms r " +
            "LEFT JOIN FETCH r.hostel " +
            "WHERE r.category.categoryId = :categoryId")
    List<Rooms> findByCategoryId(@Param("categoryId") Integer categoryId);

    // Method bổ sung: Tìm rooms available
    @Query("SELECT r FROM Rooms r " +
            "LEFT JOIN FETCH r.hostel " +
            "LEFT JOIN FETCH r.category " +
            "WHERE r.status = 'AVAILABLE'")
    List<Rooms> findAvailableRooms();

    // Đếm tổng số phòng trọ của một chủ trọ
    @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId")
    long countRoomsByOwnerId(Integer ownerId);

    // Đếm số phòng trống
    @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId AND r.status = 'unactive'")
    long countVacantRoomsByOwnerId(Integer ownerId);

    // Đếm số phòng đang thuê
    @Query("SELECT COUNT(r) FROM Rooms r WHERE r.hostel.owner.userId = :ownerId AND r.status = 'active'")
    long countRentedRoomsByOwnerId(Integer ownerId);

    @Query("""
                SELECT DISTINCT h FROM Hostel h
                LEFT JOIN FETCH h.rooms r
                LEFT JOIN FETCH r.category
                LEFT JOIN FETCH r.utilities
                LEFT JOIN FETCH r.images
                WHERE h.hostelId = :hostelId
            """)
    Optional<Hostel> findByIdWithRooms(@Param("hostelId") Integer hostelId);

    // Đếm số phòng đang cọc
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

   
}
