package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Repository.*;
import nhatroxanh.com.Nhatroxanh.Service.PostService;
import nhatroxanh.com.Nhatroxanh.Service.ReviewService;

@Controller
public class HomeController {
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private RoomsRepository roomsRepository;

    @Autowired
    private ReviewService reviewService;

    @GetMapping({ "/", "/index", "/trang-chu" })
    public String home(Model model) {
        List<Post> posts = postService.findTopApprovedActivePostsByViews(12);
        model.addAttribute("posts", posts);
        return "index";
    }

    @Transactional
    @GetMapping("/chi-tiet/{postId}")
    public String getPostDetail(@PathVariable("postId") Integer postId,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<Post> postOptional = postRepository.findById(postId);
            if (postOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bài đăng không tồn tại.");
                return "redirect:/error/404";
            }

            Post post = postOptional.get();
            if (!post.getStatus() || post.getApprovalStatus() != ApprovalStatus.APPROVED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bài đăng chưa được duyệt hoặc không hiển thị.");
                return "redirect:/error/403";
            }

            // Lấy tiện ích riêng
            Set<Utility> utilities = postRepository.findUtilitiesByPostId(postId);
            log.info("Post {} utilities (size: {}): {}", postId, utilities.size(),
                    utilities.stream().map(Utility::getName).collect(Collectors.toList()));

            // Lấy danh sách hình ảnh
            List<String> images = post.getImages() != null && !post.getImages().isEmpty()
                    ? post.getImages().stream().map(Image::getUrl).distinct().collect(Collectors.toList())
                    : List.of("/images/cards/default.jpg");
            log.info("Post {} images: {}", postId, images);

            // Fetch hostel and rooms for the post
            Hostel hostel = post.getHostel() != null
                    ? hostelRepository.findByIdWithRooms(post.getHostel().getHostelId())
                            .orElse(null)
                    : null;
            List<Rooms> rooms = hostel != null && hostel.getRooms() != null ? hostel.getRooms() : List.of();

            // Fetch reviews with pagination
            Pageable pageable = PageRequest.of(page, 5);
            Page<Review> reviews = reviewService.getReviewsByPost(postId, pageable);
            Double averageRating = reviewService.getAverageRating(postId);

            // Fetch similar posts
            List<Post> similarPosts = postRepository.findSimilarPostsByCategory(postId,
                    post.getCategory() != null ? post.getCategory().getCategoryId() : null).stream()
                    .filter(p -> p.getStatus() && p.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .limit(4)
                    .collect(Collectors.toList());

            // Add attributes to model
            model.addAttribute("post", post);
            model.addAttribute("owner", post.getUser() != null ? post.getUser() : new Users());
            model.addAttribute("utilities", utilities != null ? new HashSet<>(utilities) : new HashSet<>());
            model.addAttribute("images", images);
            model.addAttribute("hostel", hostel);
            model.addAttribute("rooms", rooms);
            model.addAttribute("roomCount", rooms.size());
            model.addAttribute("similarPosts", similarPosts);
            model.addAttribute("reviews", reviews.getContent());
            model.addAttribute("totalReviews", reviews.getTotalElements());
            model.addAttribute("averageRating", averageRating);
            model.addAttribute("hasMoreReviews", reviews.hasNext());
            model.addAttribute("currentPage", page);

            postRepository.incrementViewCount(postId);

            return "guest/chi-tiet";

        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết bài đăng {}: {}", postId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi xảy ra. Vui lòng thử lại sau.");
            return "redirect:/error/500";
        }
    }

    @GetMapping("/api/rooms/{roomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomDetails(@PathVariable Integer roomId) {
        try {
            Rooms room = roomsRepository.findByIdWithDetails(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại với ID: " + roomId));
            Set<Utility> utilities = roomsRepository.findUtilitiesByRoomId(roomId);
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("roomId", room.getRoom_id());
            roomData.put("name", room.getNamerooms());
            roomData.put("price", room.getPrice());
            roomData.put("acreage", room.getAcreage());
            roomData.put("maxTenants", room.getMax_tenants());
            roomData.put("status", room.getStatus() != null ? switch (room.getStatus()) {
                case active -> "Đã thuê";
                case unactive -> "Trống";
                case repair -> "Bảo trì";
            } : "Không xác định");
            roomData.put("category", room.getCategory() != null ? room.getCategory().getName() : "Chưa xác định");
            roomData.put("description", room.getDescription() != null ? room.getDescription() : "Chưa có mô tả");
            roomData.put("utilities", utilities != null
                    ? utilities.stream().map(Utility::getName).collect(Collectors.toList())
                    : List.of());
            roomData.put("images", room.getImages() != null && !room.getImages().isEmpty()
                    ? room.getImages().stream().map(Image::getUrl).collect(Collectors.toList())
                    : List.of("/images/cards/default.jpg"));
            return ResponseEntity.ok(roomData);
        } catch (Exception e) {
            log.error("Error loading room details for room {}: {}", roomId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}