package nhatroxanh.com.Nhatroxanh.Model.Dto;


import lombok.Data;

import java.sql.Date;

@Data
public class ContractRequestDto {
    private String tenantName;
    private Date tenantDob;
    private String tenantId;
    private Date tenantIdDate;
    private String tenantIdPlace;
    private String tenantPhone;
    private String tenantEmail;
    private String tenantAddress;

    private String ownerName;
    private Date ownerDob;
    private String ownerId;
    private Date ownerIdDate;
    private String ownerIdPlace;
    private String ownerPhone;
    private String ownerAddress;

    private String roomAddress;
    private String roomNumber;
    private Float roomArea;
    private String amenities;

    private Float rentPrice;
    private String paymentMethod;
    private String paymentDate;
    private Integer contractDuration;
    private Date startDate;
    private Float depositMonths;
    private String termsConditions;
}