package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nhatroxanh.com.Nhatroxanh.Model.entity.Contracts;
import java.sql.Date;

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
    private Date endDate;
    private Contracts.Status contractStatus;

    
}