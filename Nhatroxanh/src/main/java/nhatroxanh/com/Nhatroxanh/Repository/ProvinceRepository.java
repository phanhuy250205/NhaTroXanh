package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.entity.Province;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {
    List<Province> findAllByOrderByNameAsc();
    Optional<Province> findByCode(String code);

    Optional<Province> findByName(String name);
}