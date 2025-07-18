package nhatroxanh.com.Nhatroxanh.Model.enity;

import java.sql.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "IncidentReports")
public class IncidentReports {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reportId;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Rooms room;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "incident_type", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String incidentType;

    @Column(name = "description", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private IncidentLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IncidentStatus status;

    @Column(name = "reported_at", nullable = false)
    private Date reportedAt;

    @Column(name = "resolved_at")
    private Date resolvedAt;

    public enum IncidentLevel {
        CAO, TRUNG_BINH, THAP
    }

    public enum IncidentStatus {
        CHUA_XU_LY, DANG_XU_LY, DA_XU_LY
    }

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images;
}
