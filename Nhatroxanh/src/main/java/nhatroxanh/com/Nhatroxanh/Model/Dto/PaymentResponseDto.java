package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.sql.Timestamp; // Change from java.sql.Date
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import nhatroxanh.com.Nhatroxanh.Model.entity.Payments.PaymentMethod;
import nhatroxanh.com.Nhatroxanh.Model.entity.Payments.PaymentStatus;

@Data
@Builder
public class PaymentResponseDto {
    private Integer paymentId;
    private Integer contractId;
    private String roomCode;
    private String hostelName;
    private String tenantName;
    private String tenantPhone;
    private String month;
    private Double totalAmount;
    private Date dueDate;
    private Timestamp paymentDate; // Changed to Timestamp
    private String paymentTime; // Optional: If added for explicit time display
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private List<PaymentDetailResponseDto> details;

    @Data
    @Builder
    public static class PaymentDetailResponseDto {
        private Integer detailId;
        private String itemName;
        private Integer quantity;
        private Float unitPrice;
        private Float amount;
        private String displayText;
    }
}