package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;

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

    @Query("SELECT u.address FROM Users u WHERE u.userId = :userId")
    String findAddressByUserId(@Param("userId") Integer userId);

    @Query("SELECT u.address FROM Users u WHERE u.userId = :userId")
    Optional<Address> findAddressEntityByUserId(@Param("userId") Integer userId);

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

    @Query("SELECT u FROM Users u " +
            "WHERE u.role = :role " +
            "AND u.status = :status " +
            "AND (:enabled IS NULL OR u.enabled = :enabled) " +
            "AND (:keyword IS NULL OR LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Users> searchOwners(@Param("role") Users.Role role,
            @Param("status") Users.Status status,
            @Param("keyword") String keyword,
            @Param("enabled") Boolean enabled,
            Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    long countByRole(Users.Role role);

    long countByRoleAndEnabled(Users.Role role, boolean enabled);

    @Query("SELECT u FROM Users u WHERE u.role = :role AND " +
            "(LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Users> findByRoleAndKeyword(@Param("role") Users.Role role,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT u FROM Users u WHERE u.role = :role AND u.enabled = :enabled AND " +
            "(LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Users> findByRoleAndEnabledAndKeyword(@Param("role") Users.Role role,
            @Param("enabled") boolean enabled,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT COUNT(u) > 0 FROM Users u WHERE u.email = :email AND u.userId <> :userId")
    boolean existsByEmailAndNotUserId(@Param("email") String email, @Param("userId") Integer userId);

    @Query("SELECT COUNT(u) > 0 FROM Users u WHERE u.phone = :phone AND u.userId <> :userId")
    boolean existsByPhoneAndNotUserId(@Param("phone") String phone, @Param("userId") Integer userId);

    @Query("SELECT u FROM Users u WHERE u.role = :role AND u.enabled = :enabled AND u.bankAccount IS NOT NULL AND u.bankId IS NOT NULL")
    Optional<Users> findByRoleAndEnabledAndBankAccountIsNotNull(@Param("role") Users.Role role, @Param("enabled") boolean enabled);

    @Query("SELECT u FROM Users u WHERE u.role = :role AND u.enabled = :enabled " +
           "AND u.bankAccount IS NOT NULL AND u.bankId IS NOT NULL " +
           "AND u.accountHolderName IS NOT NULL " +
           "AND TRIM(u.bankAccount) != '' AND TRIM(u.bankId) != '' AND TRIM(u.accountHolderName) != '' " +
           "ORDER BY u.createdAt ASC")
    List<Users> findActiveStaffWithCompleteBankInfo(@Param("role") Users.Role role, @Param("enabled") boolean enabled);

    @Query("SELECT u FROM Users u WHERE u.role = :role AND u.enabled = :enabled " +
           "AND u.bankAccount IS NOT NULL AND u.bankId IS NOT NULL " +
           "AND u.accountHolderName IS NOT NULL " +
           "AND TRIM(u.bankAccount) != '' AND TRIM(u.bankId) != '' AND TRIM(u.accountHolderName) != '' " +
           "ORDER BY u.createdAt ASC")
    Optional<Users> findFirstActiveStaffWithCompleteBankInfo(@Param("role") Users.Role role, @Param("enabled") boolean enabled);

    Page<Users> findByRoleAndStatus(Users.Role role, Users.Status status, Pageable pageable);

    @Query("SELECT u FROM Users u WHERE u.role = :role AND u.status = :status AND (LOWER(u.fullname) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Users> findPendingOwnersBySearch(@Param("role") Users.Role role, @Param("status") Users.Status status,
            @Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT u FROM Users u " +
           "LEFT JOIN FETCH u.rentedContracts c " +
           "LEFT JOIN FETCH c.room r " +
           "LEFT JOIN FETCH r.hostel h " +
           "WHERE h.hostelId = :hostelId")
    List<Users> findByHostelId(@Param("hostelId") Integer hostelId);
}