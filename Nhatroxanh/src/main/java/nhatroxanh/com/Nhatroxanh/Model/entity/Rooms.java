package nhatroxanh.com.Nhatroxanh.Model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Bỏ @Data, thay bằng các annotation cụ thể
@Getter
@Setter
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

    @Column(name = "address", nullable = true)
    private String address;

    @Column(name = "acreage")
    private Float acreage;

    @Column(name = "max_tenants")
    private Integer max_tenants;

    @Column(name = "price")
    private Float price;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "RoomUtilities",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "utility_id")
    )
    private Set<Utility> utilities = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images;

    // --- PHẦN SỬA LỖI ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rooms rooms = (Rooms) o;
        // Chỉ so sánh dựa trên ID
        return roomId != null && roomId.equals(rooms.roomId);
    }

    @Override
    public int hashCode() {
        // Chỉ băm dựa trên ID
        return Objects.hash(roomId);
    }

    @Override
    public String toString() {
        // toString an toàn, không in các đối tượng liên quan trực tiếp để tránh vòng lặp
        return "Rooms{" +
                "roomId=" + roomId +
                ", hostelId=" + (hostel != null ? hostel.getHostelId() : "null") +
                ", namerooms='" + namerooms + '\'' +
                ", status=" + status +
                ", utilityIds=" + (utilities != null ? utilities.stream().map(Utility::getUtilityId).collect(Collectors.toList()) : "[]") +
                '}';
    }
}