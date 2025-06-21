package nhatroxanh.com.Nhatroxanh.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Ward;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


public interface WardRepository extends JpaRepository<Ward, Integer> {
   Optional<Ward> findByCode(String code);
}