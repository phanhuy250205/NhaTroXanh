package nhatroxanh.com.Nhatroxanh.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.TemporaryRecidence; // Giả định entity
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporaryResidenceRepository extends JpaRepository<TemporaryRecidence, Integer> {
    void deleteByAddressId(Integer addressId);
}