package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TenantSummaryDTO {
    private Integer userId;
    private String fullName;
    private String phone;
    private Long totalContracts; // tổng số phòng đã/đang thuê

    public TenantSummaryDTO(Integer userId, String fullName, String phone, Long totalContracts) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.totalContracts = totalContracts;
    }

}
