package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Repository.*;
import nhatroxanh.com.Nhatroxanh.Service.PostService;

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

    /**
     * Xử lý các yêu cầu đến trang chủ.
     * Ánh xạ các đường dẫn: "/", "/index", và "/trang-chu".
     * 
     * @param model Đối tượng Model để truyền dữ liệu đến view.
     * @return Tên của view (template) để hiển thị.
     */
    // ### THAY ĐỔI: Thêm "/index" và "/trang-chu" vào đây ###
    @GetMapping({ "/", "/index", "/trang-chu" })
    public String home(Model model) {
        // Giữ nguyên logic lấy bài đăng của bạn
        List<Post> posts = postService.findTopApprovedActivePostsByViews(12);
        model.addAttribute("posts", posts);
        return "index";
    }

    /**
     * Xử lý yêu cầu xem chi tiết một bài đăng.
     * 
     * @param postId             ID của bài đăng.
     * @param model              Đối tượng Model.
     * @param redirectAttributes Dùng để gửi thông báo lỗi khi chuyển hướng.
     * @return Tên view chi tiết hoặc chuyển hướng về trang lỗi.
     */
    @Transactional
    @GetMapping("/chi-tiet/{postId}")
    public String getPostDetail(@PathVariable("postId") Integer postId, Model model,
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

            model.addAttribute("post", post);
            model.addAttribute("owner", post.getUser() != null ? post.getUser() : new Users());
            model.addAttribute("utilities", utilities != null ? new HashSet<>(utilities) : new HashSet<>());

            // Lấy danh sách hình ảnh
            List<String> images = post.getImages() != null && !post.getImages().isEmpty()
                    ? post.getImages().stream().map(Image::getUrl).distinct().collect(Collectors.toList())
                    : List.of("/images/cards/default.jpg");
            log.info("Post {} images: {}", postId, images);
            model.addAttribute("images", images);

            // Fetch hostel and rooms for the post
            Hostel hostel = post.getHostel() != null
                    ? hostelRepository.findByIdWithRooms(post.getHostel().getHostelId())
                            .orElse(null)
                    : null;
            List<Rooms> rooms = hostel != null && hostel.getRooms() != null ? hostel.getRooms() : List.of();
            
            model.addAttribute("hostel", hostel);
            model.addAttribute("rooms", rooms);
            model.addAttribute("roomCount", rooms.size());
            List<Post> similarPosts = postRepository.findSimilarPostsByCategory(postId,
                    post.getCategory() != null ? post.getCategory().getCategoryId() : null).stream()
                    .filter(p -> p.getStatus() && p.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .limit(4)
                    .collect(Collectors.toList());
            model.addAttribute("similarPosts", similarPosts);

            postRepository.incrementViewCount(postId);

            return "guest/chi-tiet";

        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết bài đăng {}: {}", postId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi xảy ra. Vui lòng thử lại sau.");
            return "redirect:/error/500";
        }
    }

    /**
     * Lấy chi tiết phòng qua API.
     * 
     * @param roomId ID của phòng.
     * @return JSON chứa thông tin phòng hoặc lỗi.
     */
    @GetMapping("/api/rooms/{roomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomDetails(@PathVariable Integer roomId) {
        try {
            Rooms room = roomsRepository.findByIdWithDetails(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại với ID: " + roomId));
            Set<Utility> utilities = roomsRepository.findUtilitiesByRoomId(roomId); // Use the query
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("roomId", room.getRoomId());
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
