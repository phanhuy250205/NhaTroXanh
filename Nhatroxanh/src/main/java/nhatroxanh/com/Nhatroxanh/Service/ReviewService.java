package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;
import java.util.Optional;

import nhatroxanh.com.Nhatroxanh.Model.enity.Review;

public interface ReviewService {
    List<Review> getReviewsByPostId(Integer postId);

    Double getAverageRating(Integer postId);

    Review createOrUpdateReview(Review review);

    void deleteReview(Integer reviewId);

    Optional<Review> findById(Integer reviewId);

    List<Review> getReviewsByOwnerId(Integer ownerId);

    void deleteReviewById(Integer reviewId, Integer ownerId);

    List<Review> getReviewsByHostelId(Integer hostelId);
}
