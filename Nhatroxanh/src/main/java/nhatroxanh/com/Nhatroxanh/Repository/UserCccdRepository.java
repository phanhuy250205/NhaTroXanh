package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

@Repository
public interface UserCccdRepository extends JpaRepository<UserCccd, Integer> {
    UserCccd findByUser(Users user);
}