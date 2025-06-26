package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;

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

    List<Hostel> findByOwnerUserId(Integer userId);

    @Query("""
            SELECT DISTINCT h FROM Hostel h
            LEFT JOIN FETCH h.rooms r
            WHERE h.hostelId = :hostelId AND (r IS NULL OR r.status = 'unactive')
            """)
    Optional<Hostel> findByIdWithRooms(@Param("hostelId") Integer hostelId);

    @Query("SELECT COUNT(h) FROM Hostel h WHERE h.owner.userId = :ownerId")
    long countHostelsByOwnerId(Integer ownerId);
}