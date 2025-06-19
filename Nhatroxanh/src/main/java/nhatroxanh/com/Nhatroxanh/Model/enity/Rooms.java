package nhatroxanh.com.Nhatroxanh.Model.enity;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

public class Rooms {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer room_id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;

    @Column(name = "address", columnDefinition = "NVARCHAR(200)")
    private String address;
    @Column(name = "namerooms", columnDefinition = "NVARCHAR(200)")
    private String namerooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('Hoạt động', 'Ngừng hoạt đông','Đang sửa chửa')")
    private RoomStatus status;

    @Column(name = "acreage")
    private Float acreage;

    @Column(name = "max_tenants")
    private Integer max_tenants;

    @Column(name = "price")
    private Float price;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "RoomUtilities", joinColumns = @JoinColumn(name = "room_id"), inverseJoinColumns = @JoinColumn(name = "utility_id"))
    private Set<Utility> utilities;

}
