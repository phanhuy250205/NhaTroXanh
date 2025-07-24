package nhatroxanh.com.Nhatroxanh.Service;



import java.sql.Date;
import java.util.List;
import java.util.Optional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractListDto;
import nhatroxanh.com.Nhatroxanh.Model.entity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.UnregisteredTenants;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

public interface ContractService {



    @Transactional
    Contracts createContract(
            String tenantPhone, Integer roomId, Date contractDate, Date startDate,
            Date endDate, Float price, Float deposit, String terms,
            Contracts.Status status, String ownerCccd, Users tenant, UnregisteredTenants unregisteredTenant , Integer duration) throws Exception;

    @Transactional
    Contracts createContract(ContractDto contractDto, String ownerCccd, Users tenant, UnregisteredTenants unregisteredTenant) throws Exception;

    Contracts updateContract(Integer contractId, Contracts updatedContract) throws IllegalArgumentException, Exception;

    @Transactional
    Contracts updateContract(Integer contractId, ContractDto contractDto) throws Exception;

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


    // New methods for contract list
    List<ContractListDto> getAllContractsForList();
    List<ContractListDto> getContractsListByOwnerId(Integer ownerId);

    void updateStatus(Integer contractId, String newStatus);

    Contracts getContractById(Integer contractId);
    // Thêm method mới để lấy phòng theo tenant ID
    Rooms findRoomByTenantId(Long tenantId);


     List<Contracts> getMyContracts();
    Contracts createContractFromDto(ContractDto contractDto, Integer ownerId, MultipartFile cccdFrontFile, MultipartFile cccdBackFile);
}