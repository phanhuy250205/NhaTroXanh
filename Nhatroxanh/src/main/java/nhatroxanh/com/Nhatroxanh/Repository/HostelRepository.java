package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

public interface HostelRepository extends JpaRepository<Hostel, Integer> {

    

    @Query("SELECT h FROM Hostel h LEFT JOIN FETCH h.address a LEFT JOIN FETCH a.ward w LEFT JOIN FETCH w.district d LEFT JOIN FETCH d.province p WHERE h.owner.userId = :ownerId")
    List<Hostel> findByOwner_UserId(@Param("ownerId") Integer ownerId);

    @Query("SELECT h FROM Hostel h LEFT JOIN FETCH h.address a LEFT JOIN FETCH a.ward w LEFT JOIN FETCH w.district d LEFT JOIN FETCH d.province p WHERE h.hostelId = :id")
    Hostel findByIdWithAddress(@Param("id") Integer id);
    
    int countByOwner(Users owner);

     @Query("SELECT h FROM Hostel h WHERE h.hostelId = :id OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Hostel> findByHostelIdOrNameContainingIgnoreCase(@Param("id") Integer id, @Param("keyword") String keyword,
                        Pageable pageable);

    Page<Hostel> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // SỬA LẠI: Sử dụng 'street' thay vì 'address'
    List<Hostel> findByAddress_StreetContainingIgnoreCase(String street);
    
    // HOẶC sử dụng @Query để search linh hoạt hơn
    @Query("SELECT h FROM Hostel h LEFT JOIN FETCH h.address a LEFT JOIN FETCH a.ward w LEFT JOIN FETCH w.district d LEFT JOIN FETCH d.province p " +
           "WHERE UPPER(a.street) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
           "OR UPPER(w.name) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
           "OR UPPER(d.name) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
           "OR UPPER(p.name) LIKE UPPER(CONCAT('%', :keyword, '%'))")
    List<Hostel> findByAddressKeyword(@Param("keyword") String keyword);


    List<Hostel> findByOwnerUserId(Integer userId);

    @Query("""
            SELECT DISTINCT h FROM Hostel h
            LEFT JOIN FETCH h.rooms r
            WHERE h.hostelId = :hostelId AND (r IS NULL OR r.status = 'unactive')
            """)
    Optional<Hostel> findByIdWithRooms(@Param("hostelId") Integer hostelId);


    @Query("SELECT COUNT(h) FROM Hostel h WHERE h.owner.userId = :ownerId")
    long countHostelsByOwnerId(Integer ownerId);

    @Query("SELECT h FROM Hostel h LEFT JOIN FETCH h.rooms WHERE h.owner.userId = :ownerId")
    List<Hostel> findHostelsWithRoomsByOwnerId(@Param("ownerId") Integer ownerId);

    List<Hostel> findByOwner(Users owner);

     List<Hostel> findByOwnerUserIdAndNameContainingIgnoreCase(Integer ownerId, String keyword);

}
