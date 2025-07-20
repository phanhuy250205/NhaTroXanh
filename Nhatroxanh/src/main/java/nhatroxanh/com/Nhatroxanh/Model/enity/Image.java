package nhatroxanh.com.Nhatroxanh.Model.enity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "Images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = true)
    private Post post;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = true)
    private Rooms room;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = true)
    private Contracts contract;

    @ManyToOne
    @JoinColumn(name = "report_id", nullable = true)
    private IncidentReports report;

    @ManyToOne
    @JoinColumn(name = "user_cccd_id", nullable = true)
    private UserCccd userCccd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageType type;

    public enum ImageType {
        FRONT, BACK, OTHER
    }
}
