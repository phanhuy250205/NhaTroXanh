package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantDetailDTO; 
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd; 
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Service.TenantService;

@Service 
@RequiredArgsConstructor 
public class TenantServiceImpl implements TenantService {
    private final ContractsRepository contractsRepository;
    private final HostelRepository hostelRepository;

    @Autowired
    private ContractsRepository contractRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TenantInfoDTO> getTenantsForOwner(Integer ownerId, String keyword, Integer hostelId, Boolean status, Pageable pageable) {
        Page<Contracts> contractsPage = contractsRepository.findTenantsByOwnerWithFilters(ownerId, keyword, hostelId, status, pageable);
        return contractsPage.map(this::convertToTenantInfoDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hostel> getHostelsForOwner(Integer ownerId) {
        return hostelRepository.findByOwnerUserId(ownerId); 
    }
    
    @Override
    @Transactional(readOnly = true)
    public TenantDetailDTO getTenantDetailByContractId(Integer contractId) {
        Contracts contract = contractsRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));
        
        Users tenant = contract.getUser();
        Rooms room = contract.getRoom();
        Hostel hostel = room.getHostel();
        UserCccd userCccd = tenant.getUserCccd();
        String cccdNumber = (userCccd != null) ? userCccd.getCccdNumber() : "Chưa có";
        String issuePlace = (userCccd != null) ? userCccd.getIssuePlace() : "Chưa có";
        return TenantDetailDTO.builder()
                .contractId(contract.getContractId())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .terms(contract.getTerms())
                .contractStatus(contract.getStatus())
                .roomName(room.getNamerooms())
                .hostelName(hostel.getName())
                .userFullName(tenant.getFullname())
                .userGender(tenant.getGender())
                .userPhone(tenant.getPhone())
                .userBirthday(tenant.getBirthday())
                .userCccdNumber(cccdNumber)
                .userCccdMasked(maskCccd(cccdNumber))
                .userIssuePlace(issuePlace)
                .build();
    }

    @Override
    @Transactional
    public void updateContractStatus(Integer contractId, Boolean newStatus) {
    Contracts contract = contractRepository.findById(contractId)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));
    
    contract.setStatus(newStatus);
    contractRepository.save(contract);
}
    
    @Override
    @Transactional(readOnly = true)
    public Page<TenantInfoDTO> findAllForTesting(Pageable pageable) {
        // Gọi phương thức findAll() có sẵn để lấy tất cả các hợp đồng
        Page<Contracts> contractsPage = contractsRepository.findAll(pageable);
        
        // Dùng lại hàm chuyển đổi để biến đổi dữ liệu
        return contractsPage.map(this::convertToTenantInfoDTO);
    }

    private TenantInfoDTO convertToTenantInfoDTO(Contracts contract) {
        Users tenant = contract.getUser();
        Rooms room = contract.getRoom();
        Hostel hostel = room.getHostel();

        return new TenantInfoDTO(
            contract.getContractId(),
            tenant.getUserId(),
            tenant.getFullname(),
            tenant.getPhone(),
            hostel.getName(),
            room.getNamerooms(),
            contract.getStartDate(),
            contract.getStatus()
        );
    }
    
    private String maskCccd(String cccd) {
        if (cccd == null || cccd.length() < 7 || "Chưa có".equals(cccd)) {
            return cccd;
        }
        return cccd.substring(0, 3) + "******" + cccd.substring(cccd.length() - 3);
    }
}
