package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import org.springframework.stereotype.Repository;

@Repository

public interface UserRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findByEmail(String email);

    Optional<Users> findByPhone(String phone);

    @Query("SELECT u.userId FROM Users u WHERE u.role = :role")
    Page<Integer> findCustomerIds(@Param("role") Users.Role role, Pageable pageable);

    @Query("""
                SELECT DISTINCT u FROM Users u
                LEFT JOIN FETCH u.userCccd
                LEFT JOIN FETCH u.contracts c
                LEFT JOIN FETCH c.room r
                LEFT JOIN FETCH r.hostel h
                WHERE u.userId IN :userIds
            """)
    List<Users> findCustomersWithDetails(@Param("userIds") List<Integer> userIds);

}