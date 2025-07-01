package nhatroxanh.com.Nhatroxanh.Model.enity;

import java.sql.Date;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "User_CCCD")
public class UserCccd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cccd_number", length = 20, nullable = false, unique = true)
    private String cccdNumber;

    @Column(name = "issue_date")
    private Date issueDate;

    @Column(name = "issue_place", length = 100)
    private String issuePlace;

    @Column(name = "front_image_url", columnDefinition = "TEXT")
    private String frontImageUrl;

    @Column(name = "back_image_url", columnDefinition = "TEXT")
    private String backImageUrl;

    @OneToOne
    @JoinColumn(name = "user_id")
    private Users user;

    public UserCccd orElse(Object object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'orElse'");
    }
    @Override
    public int hashCode() {
        return Objects.hash(id, cccdNumber, issueDate, issuePlace); // Không bao gồm user
    }
}
