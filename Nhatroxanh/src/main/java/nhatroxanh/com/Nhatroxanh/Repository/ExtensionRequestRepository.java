package nhatroxanh.com.Nhatroxanh.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.ExtensionRequests;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtensionRequestRepository extends JpaRepository<ExtensionRequests, Integer> {
        @Query("SELECT e FROM ExtensionRequests e " +
                        "JOIN FETCH e.contract c " +
                        "JOIN FETCH c.room r " +
                        "JOIN FETCH r.hostel h " +
                        "JOIN FETCH h.owner o " +
                        "LEFT JOIN FETCH c.tenant t " +
                        "LEFT JOIN FETCH c.unregisteredTenant u " +
                        "WHERE o.userId = :ownerId AND e.status IN (PENDING, APPROVED) " +
                        "ORDER BY e.createdAt DESC")
        Page<ExtensionRequests> findAllByOwner(@Param("ownerId") Integer ownerId, Pageable pageable);

        @Query("SELECT e FROM ExtensionRequests e " +
                        "JOIN FETCH e.contract c " +
                        "JOIN FETCH c.room r " +
                        "JOIN FETCH r.hostel h " +
                        "JOIN FETCH h.owner o " +
                        "LEFT JOIN FETCH c.tenant t " +
                        "LEFT JOIN FETCH c.unregisteredTenant u " +
                        "WHERE o.userId = :ownerId " +
                        "AND (:keyword = '' OR " +
                        "(t.fullname IS NOT NULL AND LOWER(t.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "OR LOWER(c.tenantPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(r.namerooms) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<ExtensionRequests> findByOwnerIdAndKeyword(@Param("ownerId") Integer ownerId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("SELECT COUNT(e) FROM ExtensionRequests e " +
                        "WHERE e.contract.room.hostel.owner.userId = :ownerId")
        long countByContractRoomHostelOwnerUserId(@Param("ownerId") Integer ownerId);

        @Query("SELECT COUNT(e) FROM ExtensionRequests e " +
                        "JOIN e.contract c " +
                        "JOIN c.room r " +
                        "JOIN r.hostel h " +
                        "JOIN h.owner o " +
                        "LEFT JOIN c.tenant t " +
                        "LEFT JOIN c.unregisteredTenant u " +
                        "WHERE o.userId = :ownerId " +
                        "AND (:keyword = '' OR " +
                        "(t.fullname IS NOT NULL AND LOWER(t.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "OR LOWER(c.tenantPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(r.namerooms) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        long countByOwnerIdAndKeyword(@Param("ownerId") Integer ownerId, @Param("keyword") String keyword);

        boolean existsByContract_ContractIdAndStatus(Integer contractId, ExtensionRequests.RequestStatus status);
}