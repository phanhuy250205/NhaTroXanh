package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.entity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;

@Repository
public interface UserCccdRepository extends JpaRepository<UserCccd, Integer> {
    
    // Tìm UserCccd theo Users entity
    UserCccd findByUser(Users user);

    
    // Tìm UserCccd theo số CCCD (khớp với tenant-id, owner-id trong form)
    Optional<UserCccd> findByCccdNumber(String cccdNumber);
    
    // Tìm UserCccd theo user ID - CÁCH 1: Dùng naming convention
    Optional<UserCccd> findByUser_UserId(Integer userId);
    
    // Tìm UserCccd theo user ID - CÁCH 2: Dùng @Query (backup hoặc thay thế)
    @Query("SELECT uc FROM UserCccd uc WHERE uc.user.userId = :userId")
    Optional<UserCccd> findByUserId(@Param("userId") Integer userId);

     boolean existsByCccdNumber(String cccdNumber);


   

}