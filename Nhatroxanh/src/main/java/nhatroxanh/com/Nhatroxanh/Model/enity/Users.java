package nhatroxanh.com.Nhatroxanh.Model.enity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@ToString(exclude = { "notifications", "userCccd", "ownedContracts", "rentedContracts", "vouchers" })
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "users") // Đảm bảo tên bảng khớp với database
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id") // Đảm bảo tên cột khớp với database
    private Integer userId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<Notification> notifications;

    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @Column(name = "fullname", length = 100)
    private String fullname;

    @Column(name = "phone", length = 15, unique = true)
    private String phone;

    @Column(name = "birthday")
    private Date birthday;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

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

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address addressEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserCccd userCccd;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contracts> ownedContracts;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contracts> rentedContracts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<Vouchers> vouchers;

    public enum Role {
        ADMIN, STAFF, OWNER, CUSTOMER
    }

    public Users orElse(Object object) {
        throw new UnsupportedOperationException("Unimplemented method 'orElse'");
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, fullname, phone, email); // Không bao gồm userCccd
    }
}
