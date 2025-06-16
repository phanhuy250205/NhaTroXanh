package nhatroxanh.com.Nhatroxanh.Model.enity;

import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "Posts")

public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Integer postId;

    // Thông tin cơ bản
    @Column(name = "description", columnDefinition = "NVARCHAR(255)")
    private String description;

    @Column(name = "price")
    private Float price;

    @Column(name = "area")
    private Float area; // Diện tích

    @Column(name = "view")
    private Integer view;

    @Column(name = "status")
    private Boolean status; // Đang hiển thị hay không

    @Column(name = "title", columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(name = "created_at")
    private Date createdAt;

    // Trạng thái duyệt
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private Users approvedBy;

    @Column(name = "approved_at")
    private Date approvedAt;

    // Quan hệ với user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Image> images;

    // Địa chỉ riêng cho bài viết (không dùng address của Rooms)
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = true)
    private Address address;

    // Tiện ích riêng cho bài viết
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "post_utilities", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "utility_id"))
    private Set<Utility> utilities = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;
}
