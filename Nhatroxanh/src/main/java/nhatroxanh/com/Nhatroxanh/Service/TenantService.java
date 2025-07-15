package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantDetailDTO;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;

public interface TenantService {
    Page<TenantInfoDTO> getTenantsForOwner(Integer ownerId, String keyword, Integer hostelId, Contracts.Status status, Pageable pageable);
    List<Hostel> getHostelsForOwner(Integer ownerId);
    Page<TenantInfoDTO> findAllForTesting(Pageable pageable);
    void updateContractStatus(Long contractId, Boolean newStatus);
    TenantDetailDTO getTenantDetailByContractId(Long contractId);
}
