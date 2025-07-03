package nhatroxanh.com.Nhatroxanh.Model.enity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Date;
import java.time.LocalDateTime;

@Entity
@Table(name = "unregistered_tenants")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UnregisteredTenants {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // Liên kết với bảng Users (chủ trọ)

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address; // Liên kết với bảng Address

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "phone", length = 15, unique = true)
    private String phone;

    @Column(name = "cccd_number", length = 20, unique = true) // Thêm unique
    private String cccdNumber;

    @Column(name = "issue_date")
    private Date issueDate;

    @Column(name = "issue_place", length = 100)
    private String issuePlace;

    @Column(name = "birthday")
    private Date birthday;

    @Column(name = "cccd_front_url", length = 1000)
    private String cccdFrontUrl;

    @Column(name = "cccd_back_url", length = 1000)
    private String cccdBackUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status; // Thêm trường status

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Status {
        ACTIVE, INACTIVE
    }
}