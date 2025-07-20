package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Thêm Builder để dễ dàng tạo đối tượng
public class TenantDetailDTO {

    // Thông tin Hợp đồng
    private Integer contractId;
    private Date startDate;
    private Date endDate;
    private String terms; // Ghi chú hợp đồng
    private String contractStatus;
    // Thông tin Phòng
    private String roomName;
    private String hostelName;
    
    // Thông tin Khách thuê
    private String userFullName;
    private Boolean userGender;
    private String userPhone;
    private Date userBirthday;
    private String userCccdNumber;
    private String userCccdMasked; 
    private String userIssuePlace; 
}