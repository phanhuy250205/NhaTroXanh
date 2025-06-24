package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Controller.web.HomeController;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Repository.*;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.*;

@Controller
@RequestMapping("/chu-tro")
public class PostController {
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private PostService postService;

    @Autowired
    private CategoryReponsitory categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AddressService addressService;

    @Autowired
    private UtilityRepository utilityRepository;
    @Autowired
    private HostelRepository hostelRepository;
    @Autowired
    private RoomsRepository roomsRepository;

    @GetMapping("/bai-dang")
    public String showPostList(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Integer userId = userDetails.getUser().getUserId();
        List<Post> posts = postService.getPostsByUserId(userId);

        model.addAttribute("posts", posts);
        model.addAttribute("categories", categoryRepository.findAll());
        return "host/quan-ly-bai-dang";
    }

    @GetMapping("/bai-dang/tim-kiem")
    public String searchPosts(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date toDate,
            @RequestParam(required = false) String sort,
            Model model) {

        Integer userId = userDetails.getUser().getUserId();

        List<Post> posts = postService.searchPosts(keyword, categoryId, status, fromDate, toDate, sort)
                .stream()
                .filter(p -> p.getUser().getUserId().equals(userId))
                .collect(Collectors.toList());

        model.addAttribute("posts", posts);
        model.addAttribute("categories", categoryRepository.findAll());

        // Giữ lại giá trị sau khi lọc
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "host/quan-ly-bai-dang";
    }

    @PostMapping("/xoa-bai-dang/{postId}")
    public String deletePost(@PathVariable Integer postId, @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Integer userId = userDetails.getUser().getUserId();
            Post post = postService.getPostById(postId);

            if (post == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bài đăng không tồn tại.");
            } else if (!post.getUser().getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xóa bài đăng này.");
            } else {
                postService.deletePost(postId);
                redirectAttributes.addFlashAttribute("message", "Bài đăng đã được xóa thành công.");
            }
        } catch (Exception e) {
            log.error("Error deleting post {}: {}", postId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Lỗi hệ thống. Không thể xóa bài đăng. Vui lòng thử lại sau.");
        }

        return "redirect:/chu-tro/bai-dang";
    }

    @GetMapping("/chi-tiet-bai-dang/{postId}")
    public String showPostDetail(@PathVariable Integer postId, Model model, Principal principal) {
        Post post = postService.getPostById(postId);
        if (post == null) {
            return "redirect:/chu-tro/bai-dang?error=Post not found";
        }
        Set<Utility> utilities = postRepository.findUtilitiesByPostId(postId);
        log.info("Post {} utilities (size: {}): {}", postId, utilities.size(),
                utilities.stream().map(Utility::getName).collect(Collectors.toList()));
        List<String> images = post.getImages() != null && !post.getImages().isEmpty()
                ? post.getImages().stream().map(Image::getUrl).distinct().collect(Collectors.toList())
                : List.of("/images/cards/default.jpg");
        log.info("Post {} images: {}", postId, images);

        Hostel hostel = post.getHostel() != null ? hostelRepository.findByIdWithRooms(post.getHostel().getHostelId())
                .orElse(null) : null;
        List<Rooms> rooms = hostel != null && hostel.getRooms() != null ? hostel.getRooms() : List.of();

        model.addAttribute("images", images);
        model.addAttribute("post", post);
        model.addAttribute("utilities", utilities != null ? utilities : new HashSet<>());
        model.addAttribute("hostel", hostel);
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomCount", rooms.size());

        return "host/chi-tiet-bai-dang";
    }

    @PostMapping("/cap-nhat-trang-thai")
    public String updatePostStatus(@RequestParam("postId") Integer postId,
            @RequestParam("status") Boolean status,
            RedirectAttributes redirectAttributes) {
        Post post = postService.getPostById(postId);
        if (post == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài đăng.");
            return "redirect:/chu-tro/bai-dang";
        }

        post.setStatus(status);
        postService.save(post);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái bài đăng thành công.");
        return "redirect:/chu-tro/chi-tiet-bai-dang/" + postId;
    }

    @GetMapping("/api/rooms/{roomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomDetails(@PathVariable Integer roomId) {
        try {
            Rooms room = roomsRepository.findByIdWithDetails(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại với ID: " + roomId));
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
            roomData.put("utilities", room.getUtilities() != null
                    ? room.getUtilities().stream().map(Utility::getName).collect(Collectors.toList())
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

    @GetMapping("/dang-tin")
    public String showPostForm(Model model) {
        model.addAttribute("post", new Post());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("utilities", utilityRepository.findAll());
        return "host/bai-dang-host";
    }

    @PostMapping("/dang-tin")
    public String createPost(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("price") Float price,
            @RequestParam("area") Float area,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "wardId", required = false) String wardCode,
            @RequestParam(value = "street", required = false) String street,
            @RequestParam(value = "houseNumber", required = false) String houseNumber,
            @RequestParam(value = "utilities", required = false) List<Integer> utilityIds,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "hostelId", required = false) Integer hostelId,
            @RequestParam(value = "provinceId", required = false) String provinceCode,
            @RequestParam(value = "districtId", required = false) String districtCode,
            @RequestParam(value = "provinceName", required = false) String provinceName,
            @RequestParam(value = "districtName", required = false) String districtName,
            @RequestParam(value = "wardName", required = false) String wardName,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Creating post with title: {}", title);
            log.info("User: {}", userDetails.getUsername());
            log.info("Hostel ID: {}", hostelId);
            log.info("Images count: {}", images != null ? images.length : 0);

            Post post = postService.createPost(
                    title, description, price, area, categoryId,
                    wardCode, street, houseNumber, utilityIds,
                    images, hostelId, userDetails.getUser(),
                    provinceCode, districtCode, provinceName, districtName, wardName);

            log.info("Post created successfully with ID: {}", post.getPostId());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo bài đăng thành công và đang chờ duyệt!");
            redirectAttributes.addFlashAttribute("postId", post.getPostId());

            return "redirect:/chu-tro/dang-tin";

        } catch (Exception e) {
            log.error("Error creating post: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Lỗi khi tạo bài đăng: " + e.getMessage());

            // Giữ lại dữ liệu form
            redirectAttributes.addFlashAttribute("title", title);
            redirectAttributes.addFlashAttribute("description", description);
            redirectAttributes.addFlashAttribute("price", price);
            redirectAttributes.addFlashAttribute("area", area);
            redirectAttributes.addFlashAttribute("categoryId", categoryId);
            redirectAttributes.addFlashAttribute("wardId", wardCode);
            redirectAttributes.addFlashAttribute("street", street);
            redirectAttributes.addFlashAttribute("houseNumber", houseNumber);
            redirectAttributes.addFlashAttribute("hostelId", hostelId);
            redirectAttributes.addFlashAttribute("provinceId", provinceCode);
            redirectAttributes.addFlashAttribute("districtId", districtCode);
            redirectAttributes.addFlashAttribute("provinceName", provinceName);
            redirectAttributes.addFlashAttribute("districtName", districtName);
            redirectAttributes.addFlashAttribute("wardName", wardName);

            return "redirect:/chu-tro/dang-tin";
        }
    }

    @GetMapping("/api/hostels")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getHostelsByOwner(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            List<Hostel> hostels = hostelRepository.findByOwnerUserId(userDetails.getUser().getUserId());
            List<Map<String, Object>> response = hostels.stream().map(hostel -> {
                Map<String, Object> hostelData = new HashMap<>();
                hostelData.put("hostelId", hostel.getHostelId());
                hostelData.put("name", hostel.getName());

                return hostelData;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error loading hostels for user {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/sua-bai-dang/{postId}")
    public String showEditPostForm(@PathVariable Integer postId, Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Bài đăng không tồn tại với ID: " + postId));

            if (!post.getUser().getUserId().equals(userDetails.getUser().getUserId())) {
                throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa bài đăng này");
            }

            model.addAttribute("post", post);
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("utilities", utilityRepository.findAll());

            Address address = post.getAddress();
            if (address != null && address.getWard() != null) {
                Ward ward = address.getWard();
                District district = ward.getDistrict();
                Province province = district.getProvince();
                model.addAttribute("provinceId", province.getCode());
                model.addAttribute("districtId", district.getCode());
                model.addAttribute("wardId", ward.getCode());
                model.addAttribute("provinceName", province.getName());
                model.addAttribute("districtName", district.getName());
                model.addAttribute("wardName", ward.getName());
                String[] streetParts = address.getStreet().split(",", 2);
                model.addAttribute("houseNumber", streetParts.length > 0 ? streetParts[0].trim() : "");
                model.addAttribute("street", streetParts.length > 1 ? streetParts[1].trim() : address.getStreet());
            } else {
                model.addAttribute("provinceId", "");
                model.addAttribute("districtId", "");
                model.addAttribute("wardId", "");
                model.addAttribute("provinceName", "");
                model.addAttribute("districtName", "");
                model.addAttribute("wardName", "");
                model.addAttribute("houseNumber", "");
                model.addAttribute("street", "");
            }

            return "host/sua-bai-dang";
        } catch (Exception e) {
            log.error("Error loading edit post form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Lỗi khi tải form chỉnh sửa: " + e.getMessage());
            return "redirect:/chu-tro/danh-sach-bai-dang";
        }
    }

    @PostMapping("/sua-bai-dang/{postId}")
    public String updatePost(
            @PathVariable Integer postId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("price") Float price,
            @RequestParam("area") Float area,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "wardId", required = false) String wardCode,
            @RequestParam(value = "street", required = false) String street,
            @RequestParam(value = "houseNumber", required = false) String houseNumber,
            @RequestParam(value = "utilities", required = false) List<Integer> utilityIds,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "imagesToDelete", required = false) List<Integer> imagesToDelete,
            @RequestParam(value = "hostelId", required = false) Integer hostelId,
            @RequestParam(value = "provinceId", required = false) String provinceCode,
            @RequestParam(value = "districtId", required = false) String districtCode,
            @RequestParam(value = "provinceName", required = false) String provinceName,
            @RequestParam(value = "districtName", required = false) String districtName,
            @RequestParam(value = "wardName", required = false) String wardName,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Updating post with ID: {}", postId);
            Post updatedPost = postService.updatePost(
                    postId, title, description, price, area, categoryId,
                    wardCode, street, houseNumber, utilityIds, images, imagesToDelete,
                    hostelId, userDetails.getUser(), provinceCode, districtCode,
                    provinceName, districtName, wardName);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật bài đăng thành công và đang chờ duyệt!");

            return "redirect:/chu-tro/bai-dang";

        } catch (Exception e) {
            log.error("Error updating post: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Lỗi khi cập nhật bài đăng: " + e.getMessage());

            redirectAttributes.addFlashAttribute("title", title);
            redirectAttributes.addFlashAttribute("description", description);
            redirectAttributes.addFlashAttribute("price", price);
            redirectAttributes.addFlashAttribute("area", area);
            redirectAttributes.addFlashAttribute("categoryId", categoryId);
            redirectAttributes.addFlashAttribute("wardId", wardCode);
            redirectAttributes.addFlashAttribute("street", street);
            redirectAttributes.addFlashAttribute("houseNumber", houseNumber);
            redirectAttributes.addFlashAttribute("hostelId", hostelId);
            redirectAttributes.addFlashAttribute("provinceId", provinceCode);
            redirectAttributes.addFlashAttribute("districtId", districtCode);
            redirectAttributes.addFlashAttribute("provinceName", provinceName);
            redirectAttributes.addFlashAttribute("districtName", districtName);
            redirectAttributes.addFlashAttribute("wardName", wardName);

            return "redirect:/chu-tro/sua-bai-dang/" + postId;
        }
    }

}
