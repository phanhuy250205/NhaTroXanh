package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.ExtensionRequests;

public interface ExtensionRequestRepository extends JpaRepository<ExtensionRequests, Integer> {
    boolean existsByContract_ContractIdAndStatus(Integer contractId, ExtensionRequests.RequestStatus status);

     @Query("SELECT e FROM ExtensionRequests e " +
           "JOIN FETCH e.contract c " +
           "JOIN FETCH c.room r " +
           "JOIN FETCH c.owner o " +
           "LEFT JOIN FETCH c.tenant t " +
           "LEFT JOIN FETCH c.unregisteredTenant u " +
           "WHERE o.userId = :ownerId " +
           "ORDER BY e.createdAt DESC")
    List<ExtensionRequests> findAllByOwnerId(Integer ownerId);
}
