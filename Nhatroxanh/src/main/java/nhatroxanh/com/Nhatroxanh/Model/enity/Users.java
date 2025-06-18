package nhatroxanh.com.Nhatroxanh.Model.enity;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    private Integer userId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications;
    
    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @Column(name = "fullname", length = 100)
    private String fullname;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "birthday")
    private Date birthday;

    @Column(name = "gender")
    private Boolean gender;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "cccd", length = 20)
    private String cccd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expiration")
    private LocalDateTime otpExpiration;

    
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    public enum Status {
        ACTIVE, INACTIVE
    }

    public enum Role {
        admin, staff, owner, customer
    }
}
