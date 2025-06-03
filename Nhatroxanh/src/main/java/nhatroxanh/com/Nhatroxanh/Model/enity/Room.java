package nhatroxanh.com.Nhatroxanh.Model.enity;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Rooms")

public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer room_id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "room_number")
    private Integer room_number;

    @Column(name = "address", columnDefinition = "NVARCHAR(200)")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('Hoạt động', 'Ngừng hoạt đông','Đang sửa chửa')")
    private RoomStatus status;

    @Column(name = "acreage")
    private Float acreage;

    @Column(name = "max_tenants")
    private Integer max_tenants;

    @Column(name = "utilities", columnDefinition = "NVARCHAR(255)")
    private String utilities;
}
