package nhatroxanh.com.Nhatroxanh.Model.enity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.EnumType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")

public class Rooms {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer roomId;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @EqualsAndHashCode.Exclude
    private Category category;

    @ManyToOne
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;

    @Column(name = "description", columnDefinition = "NVARCHAR(200)")
    private String description;

    @Column(name = "namerooms", columnDefinition = "NVARCHAR(200)")
    private String namerooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status;

    @Column(name = "acreage")
    private Float acreage;

    @Column(name = "max_tenants")
    private Integer max_tenants;

    @Column(name = "price")
    private Float price;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "RoomUtilities", joinColumns = @JoinColumn(name = "room_id"), inverseJoinColumns = @JoinColumn(name = "utility_id"))
    private Set<Utility> utilities = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images;

    @Override
    public String toString() {
        return "Rooms{" +
                "roomId=" + roomId +
                ", category=" + category +
                ", hostel=" + hostel +
                ", description='" + description + '\'' +
                ", namerooms='" + namerooms + '\'' +
                ", status=" + status +
                ", acreage=" + acreage +
                ", max_tenants=" + max_tenants +
                ", price=" + price +
                ", utilities=" + utilities +
                ", images=" + images +
                '}';
    }

    public enum RoomStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE // Thêm các trạng thái khác nếu cần
    }
}