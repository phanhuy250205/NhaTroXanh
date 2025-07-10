package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.enity.Review;
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

    @Override
    public void deleteReviewById(Integer reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    @Override
    public List<Review> getReviewsByHostelId(Integer hostelId) {
        return reviewRepository.getReviewsByHostelId(hostelId);
    }
}
