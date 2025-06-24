package nhatroxanh.com.Nhatroxanh.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import nhatroxanh.com.Nhatroxanh.Model.enity.Review;

public interface ReviewService {
    Review createReview(Integer postId, Double rating, String comment, Integer userId);

    Review updateReview(Integer reviewId, Double rating, String comment, Integer userId);

    void deleteReview(Integer reviewId, Integer userId);

    Page<Review> getReviewsByPost(Integer postId, Pageable pageable);

    Double getAverageRating(Integer postId);
}
