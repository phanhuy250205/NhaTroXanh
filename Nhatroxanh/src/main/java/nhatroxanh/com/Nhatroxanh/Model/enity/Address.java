package nhatroxanh.com.Nhatroxanh.Model.enity;

import jakarta.persistence.*;
import lombok.*;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "Address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "street", length = 255)
    private String street;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @EqualsAndHashCode.Exclude
    private Users user;

    // Các trường khác như ward, district, province nếu có
    @ManyToOne
    @JoinColumn(name = "ward_id")
    @EqualsAndHashCode.Exclude
    private Ward ward;
}