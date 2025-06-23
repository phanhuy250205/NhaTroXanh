package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import org.springframework.stereotype.Repository;

@Repository

public interface UserRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findByEmail(String email);

    Optional<Users> findByUsername(String username);

    // Tìm người dùng theo số điện thoại (khớp với tenant-phone, owner-phone trong form)
    Optional<Users> findByPhone(String phone);

    // Tìm người dùng theo số CCCD (khớp với tenant-id, owner-id trong form)
    Optional<Users> findByCccd(String cccd);

    // Tìm người dùng theo vai trò (owner hoặc customer)
    List<Users> findByRole(Users.Role role);
}