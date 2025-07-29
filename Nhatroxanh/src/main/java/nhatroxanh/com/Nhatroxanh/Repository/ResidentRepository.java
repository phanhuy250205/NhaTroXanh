package nhatroxanh.com.Nhatroxanh.Repository;

import nhatroxanh.com.Nhatroxanh.Model.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResidentRepository extends JpaRepository<Resident, Long> {
    boolean existsByPhone(String phone);
    boolean existsByCccdNumber(String cccdNumber); 
}