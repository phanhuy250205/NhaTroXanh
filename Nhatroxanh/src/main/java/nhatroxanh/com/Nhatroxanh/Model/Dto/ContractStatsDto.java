package nhatroxanh.com.Nhatroxanh.Model.Dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractStatsDto {
    private Integer totalContracts;
    private Integer totalTenants;
    private Integer pendingPayments;
    private Float totalRevenue;
    
    // Getters for formatted values
    public String getFormattedTotalRevenue() {
        if (totalRevenue != null) {
            return String.format("%,.0f", totalRevenue);
        }
        return "0";
    }
}