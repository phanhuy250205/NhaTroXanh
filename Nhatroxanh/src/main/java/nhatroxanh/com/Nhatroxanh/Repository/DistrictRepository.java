package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.District;

@Repository
public interface DistrictRepository extends JpaRepository<District, Integer> {
}

