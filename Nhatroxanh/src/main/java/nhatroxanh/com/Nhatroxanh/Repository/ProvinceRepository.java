package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import nhatroxanh.com.Nhatroxanh.Model.enity.Province;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {
}