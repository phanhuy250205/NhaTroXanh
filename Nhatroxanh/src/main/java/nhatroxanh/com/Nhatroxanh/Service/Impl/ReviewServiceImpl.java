package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Review;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ReviewRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
}
