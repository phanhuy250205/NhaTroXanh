package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nhatroxanh.com.Nhatroxanh.Model.enity.Review;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByPostPostIdOrderByCreatedAtDesc(Integer postId);

    @Query("SELECT r FROM Review r WHERE r.post.postId = :postId ORDER BY r.createdAt DESC")
    Page<Review> findByPostIdPaged(@Param("postId") Integer postId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.post.postId = :postId AND r.user.userId = :userId")
    Optional<Review> findByPostIdAndUserId(@Param("postId") Integer postId, @Param("userId") Integer userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.post.postId = :postId")
    Double findAverageRatingByPostId(@Param("postId") Integer postId);

    @Query("SELECT r FROM Review r WHERE r.post.postId = :postId ORDER BY r.createdAt DESC")
    List<Review> findReviewsByPostId(@Param("postId") Integer postId);

    // Lấy đánh giá theo bài viết của người dùng
    @Query("SELECT r FROM Review r WHERE r.post.user.userId = :userId")
    List<Review> findByPostUserId(@Param("userId") Integer userId);

    // Lấy đánh giá theo chủ trọ của bài viết
    @Query("SELECT r FROM Review r WHERE r.post.hostel.owner.userId = :ownerId")
    List<Review> findByPostHostelOwnerId(@Param("ownerId") Integer ownerId);

    // Lấy đánh giá của các bài viết hoặc phòng mà user là chủ
    @Query("SELECT r FROM Review r WHERE r.post.user.userId = :userId OR r.room.hostel.owner.userId = :userId")
    List<Review> findAllByUserOwnPostsOrRooms(@Param("userId") Integer userId);

    @Query("SELECT r FROM Review r WHERE r.room.hostel.id = :hostelId")
    List<Review> getReviewsByHostelId(@Param("hostelId") Integer hostelId);
}