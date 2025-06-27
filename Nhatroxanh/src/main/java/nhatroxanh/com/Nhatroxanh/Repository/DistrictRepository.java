package nhatroxanh.com.Nhatroxanh.Repository;


import nhatroxanh.com.Nhatroxanh.Model.enity.District;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, Integer> {
    Optional<District> findByCode(String code);
}