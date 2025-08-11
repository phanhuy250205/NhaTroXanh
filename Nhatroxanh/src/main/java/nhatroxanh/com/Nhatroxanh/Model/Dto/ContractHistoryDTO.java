package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.time.LocalDate;

public class ContractHistoryDTO {
    private Long contractId;
    private Long userId;
    private String fullName;
    private String phone;
    private String hostelName;
    private String roomName;
    private LocalDate moveInDate;
    private LocalDate moveOutDate;
    private boolean status; 
}
