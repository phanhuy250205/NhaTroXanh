package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfoDTO {
    private Integer userId; 
    private String fullName;
    private String phone;
    private String cccdNumber;
    private String motelName;
    private String roomName;  
    private String contractStatus;
}
