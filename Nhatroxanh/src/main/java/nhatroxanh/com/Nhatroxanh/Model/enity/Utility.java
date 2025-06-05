package nhatroxanh.com.Nhatroxanh.Model.enity;

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
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "Utilities")
public class Utility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer utilityId;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany(mappedBy = "utilities")
    private Set<Rooms> rooms;
}
