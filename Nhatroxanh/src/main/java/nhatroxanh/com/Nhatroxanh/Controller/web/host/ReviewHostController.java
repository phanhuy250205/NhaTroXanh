package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Review;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.ReviewService;
import nhatroxanh.com.Nhatroxanh.Repository.ReviewRepository;

@Controller
@RequestMapping("/chu-tro/danh-gia")
@RequiredArgsConstructor
public class ReviewHostController {
    @Autowired
    private ReviewService reviewService;

    @Autowired
    private HostelService hostelService;

    @Autowired
    private ReviewRepository reviewRepository;

    @GetMapping
    public String viewReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "time", required = false) String time,
            Model model) {

        Users currentUser = userDetails.getUser();

        // Lấy danh sách khu trọ theo chủ trọ
        List<Hostel> hostels = hostelService.getHostelsByOwnerId(currentUser.getUserId());

        // Lọc theo từ khóa
        if (keyword != null && !keyword.isEmpty()) {
            hostels = hostels.stream()
                    .filter(h -> h.getName() != null && h.getName().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
        }

        // Khởi tạo các Map dữ liệu
        Map<Integer, List<Review>> hostelRoomReviewsMap = new HashMap<>();
        Map<Integer, List<Review>> hostelPostReviewsMap = new HashMap<>();

        Map<Integer, Integer> hostelRoomReviewCountMap = new HashMap<>();
        Map<Integer, Double> hostelRoomAvgRatingMap = new HashMap<>();

        Map<Integer, Integer> hostelPostReviewCountMap = new HashMap<>();
        Map<Integer, Double> hostelPostAvgRatingMap = new HashMap<>();

        Map<Integer, Integer> hostelTotalReviewCountMap = new HashMap<>();
        Map<Integer, Double> hostelTotalAvgRatingMap = new HashMap<>();

        List<Review> allPostReviewsByOwner = reviewRepository.findByPostHostelOwnerId(currentUser.getUserId());

        // Thống kê tổng
        int totalReviews = 0;
        double totalRating = 0.0;
        int fiveStarCount = 0;

        for (Hostel hostel : hostels) {
            Integer hostelId = hostel.getHostelId();

            // ==== Đánh giá phòng ====
            List<Review> roomReviews = reviewRepository.getReviewsByHostelId(hostelId);
            roomReviews = filterReviews(roomReviews, rating, time);
            hostelRoomReviewsMap.put(hostelId, roomReviews);
            hostelRoomReviewCountMap.put(hostelId, roomReviews.size());
            hostelRoomAvgRatingMap.put(hostelId, roomReviews.stream()
                    .mapToDouble(Review::getRating).average().orElse(0.0));

            // ==== Đánh giá bài đăng ====
            List<Review> postReviews = allPostReviewsByOwner.stream()
                    .filter(r -> r.getPost() != null && r.getPost().getHostel() != null &&
                            r.getPost().getHostel().getHostelId().equals(hostelId))
                    .toList();
            postReviews = filterReviews(postReviews, rating, time);
            hostelPostReviewsMap.put(hostelId, postReviews);
            hostelPostReviewCountMap.put(hostelId, postReviews.size());
            hostelPostAvgRatingMap.put(hostelId, postReviews.stream()
                    .mapToDouble(Review::getRating).average().orElse(0.0));

            // ==== Tổng hợp ====
            List<Review> allReviews = new ArrayList<>();
            allReviews.addAll(roomReviews);
            allReviews.addAll(postReviews);

            hostelTotalReviewCountMap.put(hostelId, allReviews.size());
            hostelTotalAvgRatingMap.put(hostelId, allReviews.stream()
                    .mapToDouble(Review::getRating).average().orElse(0.0));

            totalReviews += allReviews.size();
            totalRating += allReviews.stream().mapToDouble(Review::getRating).sum();
            fiveStarCount += allReviews.stream().filter(r -> r.getRating().intValue() == 5).count();
        }

        double averageRating = totalReviews > 0 ? totalRating / totalReviews : 0.0;

        // Gửi dữ liệu sang view
        model.addAttribute("hostels", hostels);

        model.addAttribute("hostelRoomReviewsMap", hostelRoomReviewsMap);
        model.addAttribute("hostelPostReviewsMap", hostelPostReviewsMap);

        model.addAttribute("hostelRoomReviewCountMap", hostelRoomReviewCountMap);
        model.addAttribute("hostelRoomAvgRatingMap", hostelRoomAvgRatingMap);

        model.addAttribute("hostelPostReviewCountMap", hostelPostReviewCountMap);
        model.addAttribute("hostelPostAvgRatingMap", hostelPostAvgRatingMap);

        model.addAttribute("hostelTotalReviewCountMap", hostelTotalReviewCountMap);
        model.addAttribute("hostelTotalAvgRatingMap", hostelTotalAvgRatingMap);

        model.addAttribute("totalHostels", hostels.size());
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("fiveStarCount", fiveStarCount);

        // Để giữ lại dữ liệu bộ lọc
        model.addAttribute("keyword", keyword);
        model.addAttribute("rating", rating);
        model.addAttribute("time", time);

        return "host/QL-danh-gia-host";
    }

    private List<Review> filterReviews(List<Review> reviews, Integer rating, String time) {
        // Lọc theo sao nếu có
        if (rating != null) {
            reviews = reviews.stream()
                    .filter(r -> r.getRating().intValue() == rating)
                    .collect(Collectors.toList());
        }

        // Sắp xếp theo thời gian
        if ("newest".equalsIgnoreCase(time)) {
            reviews.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
        } else if ("oldest".equalsIgnoreCase(time)) {
            reviews.sort((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()));
        }

        return reviews;
    }

    @PostMapping("/delete/{id}")
    public String deleteReview(@PathVariable("id") Integer reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra quyền (tùy chọn)
            reviewService.deleteReviewById(reviewId, userDetails.getUser().getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Xóa đánh giá thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xóa đánh giá này.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa đánh giá. Có lỗi xảy ra.");
        }
        return "redirect:/chu-tro/danh-gia";
    }

}
