package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.entity.Review;
import nhatroxanh.com.Nhatroxanh.Repository.ReviewRepository;
import nhatroxanh.com.Nhatroxanh.Service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;

    @Override
    public List<Review> getReviewsByPostId(Integer postId) {
        return reviewRepository.findByPostPostIdOrderByCreatedAtDesc(postId);
    }

    @Override
    public Double getAverageRating(Integer postId) {
        return reviewRepository.findAverageRatingByPostId(postId);
    }

    @Override
    public Review createOrUpdateReview(Review review) {
        return reviewRepository.save(review);
    }

    @Override
    public void deleteReview(Integer reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    @Override
    public Optional<Review> findById(Integer reviewId) {
        return reviewRepository.findById(reviewId);
    }

    @Override
    public List<Review> getReviewsByOwnerId(Integer ownerId) {
        return reviewRepository.findAllByUserOwnPostsOrRooms(ownerId);
    }

    public void deleteReviewById(Integer reviewId, Integer ownerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Đánh giá không tồn tại."));

        // Kiểm tra quyền
        if (review.getRoom() != null && review.getRoom().getHostel() != null &&
                !review.getRoom().getHostel().getOwner().getUserId().equals(ownerId) ||
                review.getPost() != null && review.getPost().getHostel() != null &&
                        !review.getPost().getHostel().getOwner().getUserId().equals(ownerId)) {
            throw new SecurityException("Bạn không có quyền xóa đánh giá này.");
        }

        reviewRepository.deleteById(reviewId);
    }

    @Override
    public List<Review> getReviewsByHostelId(Integer hostelId) {
        return reviewRepository.getReviewsByHostelId(hostelId);
    }
}
