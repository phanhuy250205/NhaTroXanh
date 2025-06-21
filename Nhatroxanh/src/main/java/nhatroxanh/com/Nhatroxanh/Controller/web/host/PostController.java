package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import java.security.Principal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
        model.addAttribute("images", images);
        model.addAttribute("post", post);
        model.addAttribute("utilities", utilities != null ? utilities : new HashSet<>());
        return "host/chi-tiet-bai-dang";
    }

    @GetMapping("/dang-tin")
    public String showPostForm(Model model) {
        model.addAttribute("post", new Post());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("utilities", utilityRepository.findAll());
        return "host/bai-dang-host";
    }

    // ===== FORM SUBMISSION - SPRING BOOT TRADITIONAL =====
    @PostMapping("/dang-tin")
    public String createPost(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("price") Float price,
            @RequestParam("area") Float area,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "wardId", required = false) Integer wardId,
            @RequestParam(value = "street", required = false) String street,
            @RequestParam(value = "houseNumber", required = false) String houseNumber,
            @RequestParam(value = "utilities", required = false) List<Integer> utilityIds,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Creating post with title: {}", title);
            log.info("User: {}", userDetails.getUser().getUsername());
            log.info("Images count: {}", images != null ? images.length : 0);

            Post post = postService.createPost(
                    title, description, price, area, categoryId,
                    wardId, street, houseNumber, utilityIds,
                    images, userDetails.getUser());

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
            redirectAttributes.addFlashAttribute("wardId", wardId);
            redirectAttributes.addFlashAttribute("street", street);
            redirectAttributes.addFlashAttribute("houseNumber", houseNumber);

            return "redirect:/chu-tro/dang-tin";
        }
    }

    @GetMapping("/api/provinces")
    @ResponseBody
    public ResponseEntity<List<Province>> getProvinces() {
        try {
            List<Province> provinces = addressService.getAllProvinces();
            return ResponseEntity.ok(provinces);
        } catch (Exception e) {
            log.error("Error loading provinces: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/districts/{provinceId}")
    @ResponseBody
    public ResponseEntity<List<District>> getDistricts(@PathVariable Integer provinceId) {
        try {
            List<District> districts = addressService.getDistrictsByProvince(provinceId);
            return ResponseEntity.ok(districts);
        } catch (Exception e) {
            log.error("Error loading districts for province {}: {}", provinceId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/wards/{districtId}")
    @ResponseBody
    public ResponseEntity<List<Ward>> getWards(@PathVariable Integer districtId) {
        try {
            List<Ward> wards = addressService.getWardsByDistrict(districtId);
            return ResponseEntity.ok(wards);
        } catch (Exception e) {
            log.error("Error loading wards for district {}: {}", districtId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
