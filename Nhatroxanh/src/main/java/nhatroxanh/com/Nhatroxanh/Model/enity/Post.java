package nhatroxanh.com.Nhatroxanh.Model.enity;

import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "Posts")
@EqualsAndHashCode(exclude = { "images", "utilities", "user", "approvedBy", "hostel", "reviews" })
@ToString(exclude = { "images", "utilities", "user", "approvedBy", "hostel", "reviews" })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Integer postId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price")
    private Float price;

    @Column(name = "area")
    private Float area;

    @Column(name = "view")
    private Integer view;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "title", columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(name = "created_at")
    private Date createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @JsonBackReference
    private Users approvedBy;

    @Column(name = "approved_at")
    private Date approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private Users user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Image> images;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = true)
    private Address address;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "post_utilities", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "utility_id"))
    private Set<Utility> utilities = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = true)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = true)
    @JsonBackReference
    private Hostel hostel;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Review> reviews;
    
}