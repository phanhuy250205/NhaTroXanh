package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Repository.*;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
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
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            // 1. Kiểm tra bài viết có tồn tại không
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

            // 2. Tiện ích
            Set<Utility> utilities = new HashSet<>(postRepository.findUtilitiesByPostId(postId));

            model.addAttribute("post", post);
            model.addAttribute("owner", post.getUser() != null ? post.getUser() : new Users());
            model.addAttribute("utilities", utilities != null ? new HashSet<>(utilities) : new HashSet<>());

            // 3. Hình ảnh
            List<String> images = post.getImages() != null && !post.getImages().isEmpty()
                    ? post.getImages().stream().map(Image::getUrl).distinct().collect(Collectors.toList())
                    : List.of("/images/cards/default.jpg");
            model.addAttribute("images", images);

            // 4. Nhà trọ & phòng
            Hostel hostel = post.getHostel() != null
                    ? hostelRepository.findByIdWithRooms(post.getHostel().getHostelId()).orElse(null)
                    : null;

            // Danh sách phòng
            List<Rooms> rooms = hostel != null && hostel.getRooms() != null ? hostel.getRooms() : List.of();

            // Debug
            log.info("Post {} utilities: {}", postId, utilities.stream().map(Utility::getName).toList());
            log.info("Rooms count: {}", rooms.size());
            rooms.forEach(room -> log.info("Room: id={}, name={}, price={}, area={}",
                    room.getRoomId(), room.getNamerooms(), room.getPrice(), room.getAcreage()));
            model.addAttribute("hostel", hostel);
            model.addAttribute("rooms", rooms);
            model.addAttribute("roomCount", rooms.size());

            // 5. Bài viết tương tự
            List<Post> similarPosts = postRepository.findSimilarPostsByCategory(postId,
                    post.getCategory() != null ? post.getCategory().getCategoryId() : null).stream()
                    .filter(p -> p.getStatus() && p.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .limit(4)
                    .collect(Collectors.toList());
            model.addAttribute("similarPosts", similarPosts);

            // 6. Đánh giá
            List<Review> reviews = reviewService.getReviewsByPostId(postId);
            Double averageRating = reviewService.getAverageRating(postId);
            model.addAttribute("reviews", reviews);
            model.addAttribute("averageRating", averageRating != null ? averageRating : 0.0);

            // ✅ Thêm currentUser để view biết ai đang đăng nhập
            model.addAttribute("currentUser", userDetails != null ? userDetails.getUser() : null);

            // 7. Tăng lượt xem
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
            roomData.put("roomId", room.getRoomId());
            roomData.put("name", room.getNamerooms());
            roomData.put("price", room.getPrice());
            roomData.put("acreage", room.getAcreage());
            roomData.put("maxTenants", room.getMax_tenants());
            roomData.put("status", room.getStatus() != null ? switch (room.getStatus()) {
                case ACTIVE -> "Đã thuê";
                case INACTIVE-> "Trống";
                case MAINTENANCE -> "Bảo trì";
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