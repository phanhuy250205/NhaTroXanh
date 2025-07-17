package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;

import nhatroxanh.com.Nhatroxanh.Model.enity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.District;

@Repository
public interface DistrictRepository extends JpaRepository<District, Integer> {
    List<District> findByProvinceId(Integer provinceId);

    Optional<District> findByCode(String code);

    List<District> findByProvinceCode(String provinceCode);

    Optional<District> findByName(String name);

    Optional<District> findByNameAndProvince(String name, Province province);
}
