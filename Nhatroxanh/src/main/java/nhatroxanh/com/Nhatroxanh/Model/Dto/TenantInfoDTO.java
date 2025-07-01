package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.sql.Date; // <-- Quan trọng: import java.sql.Date
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfoDTO {
    private Integer contractId; 
    private Integer userId; 
    private String fullName;
    private String phone;
    private String hostelName; 
    private String roomName; 
    private Date moveInDate;
    private Boolean contractStatus;
}