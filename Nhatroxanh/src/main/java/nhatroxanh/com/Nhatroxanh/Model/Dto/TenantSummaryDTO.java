package nhatroxanh.com.Nhatroxanh.Model.Dto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TenantSummaryDTO {
    private Integer userId;
    private String fullName;
    private String phone;
    private Long totalContracts;
    private boolean enabled; // ✅ thay đổi từ status sang enabled

    public TenantSummaryDTO(Integer userId, String fullName, String phone, Long totalContracts, boolean enabled) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.totalContracts = totalContracts;
        this.enabled = enabled;
    }
}


