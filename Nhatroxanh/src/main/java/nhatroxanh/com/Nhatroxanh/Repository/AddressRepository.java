package nhatroxanh.com.Nhatroxanh.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;

public interface AddressRepository extends JpaRepository<Address, Integer> {
}
