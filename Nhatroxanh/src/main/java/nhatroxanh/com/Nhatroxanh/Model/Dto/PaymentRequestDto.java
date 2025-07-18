package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments.PaymentMethod;

import java.sql.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    private Integer contractId;
    private String month; // Format: "MM/yyyy"
    private Date dueDate;
    private PaymentMethod paymentMethod;
    private String notes;
    
    // Payment details
    private List<PaymentDetailDto> details;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetailDto {
        private String itemName;
        private Integer quantity;
        private Float unitPrice;
        private Float amount;
        
        // For utilities with meter readings
        private Integer previousReading;
        private Integer currentReading;
    }
}
