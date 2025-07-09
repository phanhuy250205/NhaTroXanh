
package nhatroxanh.com.Nhatroxanh.Model.enity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "contracts")
public class Contracts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId; // Đổi sang Long để thống nhất

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Rooms room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Users tenant; // Có thể null nếu dùng unregisteredTenant

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Users owner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unregistered_tenant_id")
    private UnregisteredTenants unregisteredTenant; // Có thể null nếu dùng tenant

    @Column(name = "contract_date", nullable = false)
    private Date contractDate;

    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Column(name = "start_date", nullable = false)
    private Date startDate;

    @Column(name = "end_date", nullable = false)
    private Date endDate;



    @Column(name = "price", nullable = false)
    private Float price;

    @Column(name = "deposit", nullable = false)
    private Float deposit;

    @Column(name = "duration", nullable = false)
    private Float duration;


    @Column(name = "terms", length = 1000) // Tăng giới hạn để khớp với ContractServiceImpl
    private String terms;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "tenant_phone", nullable = false)
    private String tenantPhone;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payments> payments;

    public enum Status {
        DRAFT, ACTIVE, TERMINATED, EXPIRED
    }
}
