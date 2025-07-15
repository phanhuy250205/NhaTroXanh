package nhatroxanh.com.Nhatroxanh.Service;



import java.sql.Date;
import java.util.List;
import java.util.Optional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractListDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.UnregisteredTenants;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import org.springframework.transaction.annotation.Transactional;

public interface ContractService {



    @Transactional
    Contracts createContract(
            String tenantPhone, Integer roomId, Date contractDate, Date startDate,
            Date endDate, Float price, Float deposit, String terms,
            Contracts.Status status, String ownerCccd, Users tenant, UnregisteredTenants unregisteredTenant , Integer duration) throws Exception;

    @Transactional
    Contracts createContract(ContractDto contractDto, String ownerCccd, Users tenant, UnregisteredTenants unregisteredTenant) throws Exception;

    Contracts updateContract(Long contractId, Contracts updatedContract) throws IllegalArgumentException, Exception;

    @Transactional
    Contracts updateContract(Long contractId, ContractDto contractDto) throws Exception;

    void deleteContract(Long contractId) throws Exception;

    Optional<Contracts> findContractById(Long contractId);

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


    // New methods for contract list
    List<ContractListDto> getAllContractsForList();
    List<ContractListDto> getContractsListByOwnerId(Integer ownerId);

    void updateStatus(Long contractId, String newStatus);

    Contracts getContractById(Long contractId);




    List<Contracts> getMyContracts();
}