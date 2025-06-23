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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Review createReview(Integer postId, Double rating, String comment, Integer userId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        Optional<Users> userOpt = userRepository.findById(userId);

        if (!postOpt.isPresent()) {
            throw new IllegalArgumentException("Post not found");
        }
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found");
        }
        if (reviewRepository.existsByPostAndUser(postOpt.get(), userOpt.get())) {
            throw new IllegalStateException("User has already reviewed this post");
        }
        if (rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1.0 and 5.0");
        }
        if (comment != null && comment.length() > 1000) {
            throw new IllegalArgumentException("Comment cannot exceed 1000 characters");
        }

        Review review = Review.builder()
                .post(postOpt.get())
                .user(userOpt.get())
                .rating(rating)
                .comment(comment)
                .build();

        return reviewRepository.save(review);
    }

    @Override
    public Page<Review> getReviewsByPost(Integer postId, Pageable pageable) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new IllegalArgumentException("Post not found");
        }
        return reviewRepository.findByPost(postOpt.get(), pageable);
    }

    @Override
    public Double getAverageRating(Integer postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new IllegalArgumentException("Post not found");
        }
        Page<Review> reviews = reviewRepository.findByPost(postOpt.get(), Pageable.unpaged());
        return reviews.getContent().stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
    }
}
