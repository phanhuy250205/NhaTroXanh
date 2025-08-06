package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.Data;
import nhatroxanh.com.Nhatroxanh.Model.entity.Contracts.Status;

import java.sql.Date;

@Data
public class TenantRoomHistoryDTO {
    private Integer contractId;
    private String hostelName;
    private String roomName;
    private Date startDate;
    private Date endDate;
    private String status;
    private String terms;


    public TenantRoomHistoryDTO(Integer contractId, String hostelName, String roomName,
            Date startDate, Date endDate, Status status,String terms) {
        this.contractId = contractId;
        this.hostelName = hostelName;
        this.roomName = roomName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status.name();
        this.terms = terms; 
    }
}
