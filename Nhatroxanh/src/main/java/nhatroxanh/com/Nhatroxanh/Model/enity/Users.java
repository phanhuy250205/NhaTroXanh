package nhatroxanh.com.Nhatroxanh.Model.enity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
// ✅ SỬA: Thêm addressEntity vào exclude
@ToString(exclude = {
        "notifications",
        "userCccd",
        "ownedContracts",
        "rentedContracts",
        "vouchers",
        "addressEntity" // ✅ THÊM DÒNG NÀY
})
// ✅ SỬA: Chỉ dùng userId làm key cho equals/hashCode
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @EqualsAndHashCode.Include // ✅ THÊM annotation này
    private Integer userId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications;

    @Column(name = "password", nullable = false, length = 256)
    @JsonIgnore
    private String password;

    @Column(name = "fullname", length = 100)
    private String fullname;

    @Column(name = "phone", length = 15, unique = true)
    private String phone;

    @Column(name = "birthday")
    private Date birthday;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(name = "bank_id", length = 10)
    private String bankId;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Column(name = "balance", nullable = false)
    @Builder.Default
    private Double balance = 0.0;

    @Column(name = "gender")
    private Boolean gender;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "avatar", length = 1000)
    private String avatar;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expiration")
    private LocalDateTime otpExpiration;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // @ManyToOne
    // @JoinColumn(name = "address_id")
    // private Address addressEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private UserCccd userCccd;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Contracts> ownedContracts;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Contracts> rentedContracts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JsonIgnore
    private List<Vouchers> vouchers;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    public enum Role {
        ADMIN, STAFF, OWNER, CUSTOMER
    }

    public Users orElse(Object object) {
        throw new UnsupportedOperationException("Unimplemented method 'orElse'");
    }

    public enum Status  {
        PENDING, APPROVED, REJECTED
    }

    // ✅ XÓA method hashCode() tự viết vì đã có @EqualsAndHashCode
    // @Override
    // public int hashCode() {
    // return Objects.hash(userId, fullname, phone, email);
    // }
}
