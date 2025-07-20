package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.entity.FavoritePost;
import nhatroxanh.com.Nhatroxanh.Model.entity.Post;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritePostRepository extends JpaRepository<FavoritePost, Integer> {
    
    // Kiểm tra xem user đã yêu thích post này chưa
    boolean existsByUserAndPost(Users user, Post post);
    
    // Tìm favorite post theo user và post
    Optional<FavoritePost> findByUserAndPost(Users user, Post post);
    
    // Lấy danh sách post yêu thích của user (chỉ lấy những post chưa bị ẩn)
    @Query("SELECT fp.post FROM FavoritePost fp WHERE fp.user = :user AND fp.post.status = true ORDER BY fp.createdAt DESC")
    Page<Post> findFavoritePostsByUser(@Param("user") Users user, Pageable pageable);
    
    // Lấy danh sách post yêu thích của user (List) (chỉ lấy những post chưa bị ẩn)
    @Query("SELECT fp.post FROM FavoritePost fp WHERE fp.user = :user AND fp.post.status = true ORDER BY fp.createdAt DESC")
    List<Post> findFavoritePostsByUser(@Param("user") Users user);
    
    // Đếm số lượng post yêu thích của user (chỉ đếm những post chưa bị ẩn)
    @Query("SELECT COUNT(fp) FROM FavoritePost fp WHERE fp.user = :user AND fp.post.status = true")
    long countByUser(@Param("user") Users user);
    
    // Đếm số lượng user yêu thích post (chỉ đếm cho những post chưa bị ẩn)
    @Query("SELECT COUNT(fp) FROM FavoritePost fp WHERE fp.post = :post AND fp.post.status = true")
    long countByPost(@Param("post") Post post);
    
    // Xóa favorite theo user và post
    void deleteByUserAndPost(Users user, Post post);
    
    // Lấy danh sách user ID đã yêu thích các post
    @Query("SELECT fp.user.userId FROM FavoritePost fp WHERE fp.post.postId IN :postIds")
    List<Integer> findUserIdsByPostIds(@Param("postIds") List<Integer> postIds);
}
