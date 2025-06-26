package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findByEmail(String email);
    Optional<Users> findByCccd(String cccd);
    Optional<Users> findByPhone(String phone);
    List<Users> findByRole(Users.Role role);

    default Optional<Users> findByCccdOrPhone(String cccd, String phone) {
        Optional<Users> byCccd = findByCccd(cccd);
        if (byCccd.isPresent()) {
            return byCccd;
        }
        return findByPhone(phone);
    }
    @Query("SELECT u FROM Users u WHERE (u.cccd = :cccd OR u.phone = :phone) AND u.role = :role")
    Users findByCccdOrPhoneAndRole(@Param("cccd") String cccd, @Param("phone") String phone, @Param("role") String role);
}