package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import nhatroxanh.com.Nhatroxanh.Model.entity.UnregisteredTenants;

import java.util.Optional;

public interface UnregisteredTenantsRepository  extends JpaRepository<UnregisteredTenants, Integer> {

    Optional<UnregisteredTenants> findByCccdNumber(String cccdNumber);
    Optional<UnregisteredTenants> findByPhone(String phone);
}
