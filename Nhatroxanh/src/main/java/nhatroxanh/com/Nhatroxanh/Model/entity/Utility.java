package nhatroxanh.com.Nhatroxanh.Model.entity;

import java.util.HashSet;
import java.util.Objects; // <-- Thêm import này
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;   // <-- Sửa
import lombok.NoArgsConstructor;
import lombok.Setter;   // <-- Sửa

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter // <-- Sửa: Dùng Getter và Setter riêng thay cho @Data
@Setter // <-- Sửa
@Entity
@Table(name = "Utilities")
public class Utility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer utilityId;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany(mappedBy = "utilities")
    private Set<Rooms> rooms = new HashSet<>();

    @ManyToMany(mappedBy = "utilities")
    private Set<Post> posts = new HashSet<>();
    
    // 🔥 BẮT ĐẦU PHẦN SỬA LỖI 🔥
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Utility utility = (Utility) o;
        // Chỉ so sánh dựa trên ID, và đảm bảo ID không null
        return utilityId != null && utilityId.equals(utility.utilityId);
    }

    @Override
    public int hashCode() {
        // Chỉ băm (hash) dựa trên ID
        return Objects.hash(utilityId);
    }
    // 🔥 KẾT THÚC PHẦN SỬA LỖI 🔥
}