package nhatroxanh.com.Nhatroxanh.Model.enity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@ToString(exclude = "notifications")
@Table(name = "Users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId")
    private Integer userId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
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

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserCccd userCccd;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address addressEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    public enum Role {
        ADMIN, STAFF, OWNER, CUSTOMER
    }

    public Users orElse(Object object) {
        throw new UnsupportedOperationException("Unimplemented method 'orElse'");
    }
}