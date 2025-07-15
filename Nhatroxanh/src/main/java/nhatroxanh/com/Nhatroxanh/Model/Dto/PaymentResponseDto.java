package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments.PaymentMethod;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments.PaymentStatus;

import java.sql.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Integer paymentId;
    private Long contractId;
    private String roomCode;
    private String hostelName;
    private String tenantName;
    private String tenantPhone;
    private String month;
    private Float totalAmount;
    private Date dueDate;
    private Date paymentDate;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String notes;
    
    // Payment breakdown
    private List<PaymentDetailResponseDto> details;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetailResponseDto {
        private Integer detailId;
        private String itemName;
        private Integer quantity;
        private Float unitPrice;
        private Float amount;
        
        // For display purposes
        private String displayText; // e.g., "50kWh", "10 m³"
    }
}
