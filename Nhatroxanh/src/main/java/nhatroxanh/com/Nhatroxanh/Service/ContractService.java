package nhatroxanh.com.Nhatroxanh.Service;



import java.sql.Date;
import java.util.List;
import java.util.Optional;

import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;

public interface ContractService {

    Contracts createContract(
            String tenantPhone, Integer roomId, Date contractDate, Date startDate,
            Date endDate, Float price, Float deposit, String terms,
            Contracts.Status status, String ownerId) throws Exception;

    Contracts updateContract(Integer contractId, Contracts updatedContract) throws IllegalArgumentException, Exception;

    void deleteContract(Integer contractId) throws Exception;

    Optional<Contracts> findContractById(Integer contractId);

    List<Contracts> findContractsByRoomId(Integer roomId);

    List<Contracts> findContractsByTenantUserId(Integer tenantUserId);

    List<Contracts> findContractsByOwnerId(Integer ownerId);

    List<Contracts> findContractsByStatus(Contracts.Status status);

    List<Contracts> findContractsByTenantName(String name);

    List<Contracts> findContractsByTenantPhone(String phone);

    List<Contracts> findContractsByTenantCccd(String cccd);

    List<Contracts> findContractsByDateRange(Date startDate, Date endDate);

    List<Contracts> findContractsExpiringWithin30Days();

    Optional<Contracts> findActiveContractByRoomId(Integer roomId);

    Long countContractsByOwnerId(Integer ownerId);

    Long countContractsByOwnerIdAndStatus(Integer ownerId, Contracts.Status status);

    Float getTotalRevenueByOwnerId(Integer ownerId);

    List<Contracts> findContractsByOwnerCccd(String cccd);
}