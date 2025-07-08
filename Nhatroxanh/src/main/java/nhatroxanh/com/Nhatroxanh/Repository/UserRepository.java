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
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {
        Optional<Users> findByEmail(String email);

        Optional<Users> findByPhone(String phone);

        List<Users> findByRole(Users.Role role);

        @Query("SELECT u FROM Users u LEFT JOIN u.userCccd uc WHERE (uc.cccdNumber = :cccd OR u.phone = :phone) AND u.role = :role")
        Optional<Users> findByCccdOrPhoneAndRole(
                        @Param("cccd") String cccd,
                        @Param("phone") String phone,
                        @Param("role") Users.Role role);

        @Query("SELECT uc FROM UserCccd uc WHERE uc.user.userId = :userId")
        Optional<UserCccd> findUserCccdByUserId(@Param("userId") Integer userId);

        @Query("SELECT a FROM Address a LEFT JOIN FETCH a.ward w LEFT JOIN FETCH w.district d LEFT JOIN FETCH d.province p WHERE a.id = (SELECT u.addressEntity.id FROM Users u WHERE u.userId = :userId)")
        Optional<Address> findAddressByUserId(@Param("userId") Integer userId);

        @Query("SELECT u.userId FROM Users u WHERE u.role = :role")
        Page<Integer> findCustomerIds(@Param("role") Users.Role role, Pageable pageable);


        @Query("""
                            SELECT DISTINCT u FROM Users u
                            LEFT JOIN FETCH u.userCccd
                            LEFT JOIN FETCH u.rentedContracts c
                            LEFT JOIN FETCH c.room r
                            LEFT JOIN FETCH r.hostel h
                            WHERE u.userId IN :userIds
                        """)
        List<Users> findCustomersWithDetails(@Param("userIds") List<Integer> userIds);

        @Query("SELECT u FROM Users u WHERE u.role = :role "
                        + "AND (:keyword IS NULL OR u.fullname LIKE %:keyword% OR u.email LIKE %:keyword% OR u.phone LIKE %:keyword%) "
                        + "AND (:enabled IS NULL OR u.enabled = :enabled)")
        Page<Users> searchOwners(
                        @Param("role") Users.Role role,
                        @Param("keyword") String keyword,
                        @Param("enabled") Boolean enabled,
                        Pageable pageable);

        boolean existsByEmail(String email);

        boolean existsByPhone(String phone);

}