package nhatroxanh.com.Nhatroxanh.Controller.web;

import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Review;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Date;
import java.util.Optional;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private PostRepository postRepository;

    @PostMapping
    public String createReview(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Integer postId,
            @RequestParam String rating,
            @RequestParam String comment,
            RedirectAttributes redirectAttributes) {
        logger.info("Received review request for postId: {}, rating: {}, comment: {}", postId, rating, comment);

        if (userDetails == null) {
            logger.warn("User not authenticated");
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập để gửi đánh giá.");
            return "redirect:/login";
        }

        try {
            // Kiểm tra user hợp lệ
            if (userDetails.getUser() == null || userDetails.getUser().getUserId() == null) {
                logger.error("Invalid user details: user or userId is null");
                redirectAttributes.addFlashAttribute("error", "Thông tin người dùng không hợp lệ.");
                return "redirect:/chi-tiet/" + postId;
            }

            Double ratingValue = Double.parseDouble(rating);
            if (ratingValue < 1.0 || ratingValue > 5.0) {
                logger.warn("Invalid rating value: {}", ratingValue);
                redirectAttributes.addFlashAttribute("error", "Điểm đánh giá phải từ 1 đến 5.");
                return "redirect:/chi-tiet/" + postId;
            }

            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isEmpty()) {
                logger.error("Post not found for postId: {}", postId);
                redirectAttributes.addFlashAttribute("error", "Bài đăng không tồn tại.");
                return "redirect:/chi-tiet/" + postId;
            }

            Review review = new Review();
            review.setPost(postOpt.get());
            review.setUser(userDetails.getUser());
            review.setRating(ratingValue);
            review.setComment(comment);
            review.setCreatedAt(new Date(System.currentTimeMillis()));

            reviewService.createOrUpdateReview(review);
            logger.info("Review saved successfully for postId: {}", postId);
            redirectAttributes.addFlashAttribute("message", "Đánh giá đã được gửi thành công!");
        } catch (NumberFormatException e) {
            logger.error("Invalid rating format: {}", rating, e);
            redirectAttributes.addFlashAttribute("error", "Điểm đánh giá không hợp lệ.");
        } catch (Exception e) {
            logger.error("Error processing review for postId: {}. Error: {}", postId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Đã có lỗi xảy ra: " + e.getMessage());
        }
        logger.info("Redirecting to /chi-tiet/{}", postId);
        return "redirect:/chi-tiet/" + postId;
    }

    @PostMapping("/action")
    public String handleReviewAction(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String actionType,
            @RequestParam Integer postId,
            @RequestParam(required = false) Integer reviewId,
            @RequestParam(required = false) String rating,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null || userDetails.getUser() == null) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập.");
            return "redirect:/login";
        }

        try {
            if (reviewId == null) {
                redirectAttributes.addFlashAttribute("error", "Thiếu ID đánh giá.");
                return "redirect:/chi-tiet/" + postId;
            }
            System.out.println("Review ID: " + reviewId);

            Optional<Review> reviewOpt = reviewService.findById(reviewId);
            if (reviewOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Đánh giá không tồn tại.");
                return "redirect:/chi-tiet/" + postId;
            }

            Review review = reviewOpt.get();
            Integer currentUserId = userDetails.getUser().getUserId();
            if (!currentUserId.equals(review.getUser().getUserId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thao tác với đánh giá này.");
                return "redirect:/chi-tiet/" + postId;
            }

            if ("delete".equals(actionType)) {
                reviewService.deleteReview(reviewId);
                redirectAttributes.addFlashAttribute("message", "Xóa đánh giá thành công!");
            } else if ("edit".equals(actionType)) {
                if (rating == null || comment == null) {
                    redirectAttributes.addFlashAttribute("error", "Thiếu thông tin chỉnh sửa.");
                    return "redirect:/chi-tiet/" + postId;
                }

                Double ratingVal = Double.parseDouble(rating);
                if (ratingVal < 1.0 || ratingVal > 5.0) {
                    redirectAttributes.addFlashAttribute("error", "Số sao không hợp lệ.");
                    return "redirect:/chi-tiet/" + postId;
                }

                review.setRating(ratingVal);
                review.setComment(comment);
                reviewService.createOrUpdateReview(review);
                redirectAttributes.addFlashAttribute("message", "Cập nhật đánh giá thành công!");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Đã có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/chi-tiet/" + postId;
    }

}