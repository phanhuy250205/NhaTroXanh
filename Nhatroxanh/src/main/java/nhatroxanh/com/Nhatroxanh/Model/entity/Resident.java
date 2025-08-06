// Tạo file mới: nhatroxanh/com/Nhatroxanh/Model/entity/Resident.java

package nhatroxanh.com.Nhatroxanh.Model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "residents")
public class Resident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "birth_year")
    private String birthYear;

    @Column(name = "phone")
    private String phone;

    @Column(name = "cccd_number")
    private String cccdNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contracts contract;
}