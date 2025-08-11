package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TenantSummaryDTO {

    private Integer contractId; // ID của hợp đồng mới nhất
    private Integer userId;     // ID của khách thuê hoặc người bảo hộ
    private String fullName;
    private String phone;
    private Long totalContracts;
    private String tenantType;

    // Constructor khớp với câu query mới
    public TenantSummaryDTO(Integer contractId, Integer userId, String fullName, String phone, Long totalContracts) {
        this.contractId = contractId;
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.totalContracts = totalContracts;
    }
}