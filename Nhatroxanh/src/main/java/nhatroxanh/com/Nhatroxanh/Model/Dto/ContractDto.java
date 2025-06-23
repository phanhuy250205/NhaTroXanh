package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.sql.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder  
@NoArgsConstructor
@AllArgsConstructor
public class ContractDto {
    private Integer contractId;
    private Integer roomId;
    private String roomName;
    private String roomAddress;
    private Integer userId;
    private String userName;
    private String userFullname;
    private String userPhone;
    private String userEmail;
    private Date startDate;
    private Date endDate;
    private Float price;
    private Float deposit;
    private String terms;
    private Boolean status;
    private List<String> imageUrls;
    private String statusText;
    private String statusBadgeClass;
    
    // Getter for formatted price
    public String getFormattedPrice() {
        if (price != null) {
            return String.format("%,.0f", price);
        }
        return "0";
    }
    
    // Getter for formatted deposit
    public String getFormattedDeposit() {
        if (deposit != null) {
            return String.format("%,.0f", deposit);
        }
        return "0";
    }
}