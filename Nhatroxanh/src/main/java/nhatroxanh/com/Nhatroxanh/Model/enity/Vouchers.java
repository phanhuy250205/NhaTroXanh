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
@Table(name = "Vouchers")

public class Vouchers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code")
    private String code;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = true)
    private Rooms room;
    @ManyToOne

    @JoinColumn(name = "hostel_id", nullable = true)

    private Hostel hostel;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "discount_type")
    private Boolean discountType;

    @Column(name = "discount_value")
    private Float discountValue;

    @Column(name = "start_date")
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "minAmount")
    private Float minAmount;

    @Column(name = "formatted_discount", insertable = false, updatable = false)
    private String formattedDiscount;

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_status")
    private VoucherStatus voucherStatus;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private Users approvedBy;

    public String getFormattedDiscount() {
        if (Boolean.TRUE.equals(discountType)) {
            return discountValue.intValue() + "%";
        } else {
            return String.format("%,.0fđ", discountValue);
        }
    }

}
