package nhatroxanh.com.Nhatroxanh.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.UnregisteredTenants;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnregisteredTenantsRepository  extends JpaRepository<UnregisteredTenants, Integer> {

    Optional<UnregisteredTenants> findByCccdNumber(String cccdNumber);
    Optional<UnregisteredTenants> findByPhone(String phone);
}
