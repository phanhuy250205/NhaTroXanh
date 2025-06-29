package nhatroxanh.com.Nhatroxanh.Repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;

public interface AddressRepository extends JpaRepository<Address, Integer> {

//    Optional<Address> findByUserId(Integer userId);

    Optional<Address> findByUserUserId(Integer userId);
}
