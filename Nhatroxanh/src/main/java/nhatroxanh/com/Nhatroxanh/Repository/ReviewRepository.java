package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Review;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    Page<Review> findByPost(Post post, Pageable pageable);

    boolean existsByPostAndUser(Post post, Users user);
}