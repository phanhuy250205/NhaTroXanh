package nhatroxanh.com.Nhatroxanh.Repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.Ward;

public interface WardRepository extends JpaRepository<Ward, Integer> {
    @Query("SELECT w FROM Ward w LEFT JOIN FETCH w.district d LEFT JOIN FETCH d.province p WHERE w.id = :id")
    Optional<Ward> findByIdWithDetails(@Param("id") Integer id);
    List<Ward> findByDistrictId(Integer districtId);
}
