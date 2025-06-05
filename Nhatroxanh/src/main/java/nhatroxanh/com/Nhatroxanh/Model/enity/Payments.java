package nhatroxanh.com.Nhatroxanh.Model.enity;

import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "Payments")
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "payment_id")
    private int paymentId;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contracts contract;

    @Column(name = "total_amount")
    private Float totalAmount;

    @Column(name = "due_date")
    private Date dueDate;

    @Column(name = "payment_date")
    private Date paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    public enum PaymentStatus {
        CHƯA_THANH_TOÁN, ĐÃ_THANH_TOÁN, QUÁ_HẠN_THANH_TOÁN
    }

    public enum PaymentMethod {
        TIỀN_MẶT, CHUYỂN_KHOẢN_NGÂN_HÀNG, MOMO
    }
}
