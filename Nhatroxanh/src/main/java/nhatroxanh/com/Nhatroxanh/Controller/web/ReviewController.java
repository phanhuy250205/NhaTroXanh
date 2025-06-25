package nhatroxanh.com.Nhatroxanh.Controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.enity.Review;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.PostService;
import nhatroxanh.com.Nhatroxanh.Service.ReviewService;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private PostService postService;

    @PostMapping("/create")
    public String createReview(
            @RequestParam Integer postId,
            @RequestParam Double rating,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Integer userId = userDetails.getUser().getUserId();
            reviewService.createReview(postId, rating, comment, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá đã được gửi thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi gửi đánh giá. Vui lòng thử lại!");
        }
        return "redirect:/chi-tiet/" + postId;
    }

    @PostMapping("/update/{reviewId}")
    public String updateReview(
            @PathVariable Integer reviewId,
            @RequestParam Integer postId,
            @RequestParam Double rating,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Integer userId = userDetails.getUser().getUserId();
            reviewService.updateReview(reviewId, rating, comment, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá đã được cập nhật thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền chỉnh sửa đánh giá này!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Có lỗi xảy ra khi cập nhật đánh giá. Vui lòng thử lại!");
        }
        return "redirect:/chi-tiet/" + postId;
    }

    @PostMapping("/delete/{reviewId}")
    public String deleteReview(
            @PathVariable Integer reviewId,
            @RequestParam Integer postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Integer userId = userDetails.getUser().getUserId();
            reviewService.deleteReview(reviewId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá đã được xóa thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xóa đánh giá này!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xóa đánh giá. Vui lòng thử lại!");
        }
        return "redirect:/chi-tiet/" + postId;
    }

    // API endpoint for loading more reviews via AJAX
    @GetMapping("/api/reviews/{postId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMoreReviews(
            @PathVariable Integer postId,
            @RequestParam(defaultValue = "0") int page) {
        try {
            Pageable pageable = PageRequest.of(page, 5);
            Page<Review> reviews = reviewService.getReviewsByPost(postId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews.getContent());
            response.put("hasMore", reviews.hasNext());
            response.put("totalPages", reviews.getTotalPages());
            response.put("currentPage", page);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Không thể tải đánh giá: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
