package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import java.util.List;


public interface UserRepository extends JpaRepository<Users, Integer> {
       Optional<Users> findByEmail(String email);
    Optional<Users> findByUsername(String username);
    
}
