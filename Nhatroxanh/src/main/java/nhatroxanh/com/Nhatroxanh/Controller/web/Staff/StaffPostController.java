package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.CategoryService;
import nhatroxanh.com.Nhatroxanh.Service.PostService;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/nhan-vien/bai-dang")
public class StaffPostController {

    private static final Logger log = LoggerFactory.getLogger(StaffPostController.class);

    @Autowired
    private PostService postService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private HostelRepository hostelRepository;

    // Trang danh sách bài đăng
    @GetMapping
    public String showPostManagement(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String search,
            Model model) {

        List<Post> posts = postService.getFilteredPosts(status, type, sortBy, search);

        List<Post> pendingPosts = posts.stream()
                .filter(post -> post.getApprovalStatus() == ApprovalStatus.PENDING)
                .toList();

        List<Post> approvedPosts = posts.stream()
                .filter(post -> post.getApprovalStatus() == ApprovalStatus.APPROVED)
                .toList();

        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("pendingPosts", pendingPosts);
        model.addAttribute("approvedPosts", approvedPosts);
        model.addAttribute("categories", categories);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("search", search);

        return "staff/bai-dang";
    }

    // Duyệt bài đăng
    @PostMapping("/{postId}/approve")
    public String approvePost(
            @PathVariable Integer postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        // Debug check nếu userDetails null
        if (userDetails == null) {
            log.warn("Không lấy được thông tin người dùng từ @AuthenticationPrincipal.");
            redirectAttributes.addFlashAttribute("errorMessage", "Không xác định được người dùng đang đăng nhập.");
            return "redirect:/nhan-vien/bai-dang";
        }

        try {
            postService.approvePost(postId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Bài đăng đã được duyệt!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi duyệt bài đăng.");
        }
        return "redirect:/nhan-vien/bai-dang";
    }

    // Ẩn bài đăng
    @PostMapping("/cap-nhat-trang-thai")
    public String updatePostStatus(@RequestParam("postId") Integer postId,
            RedirectAttributes redirectAttributes) {
        Post post = postService.getPostById(postId);
        if (post == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài đăng.");
            return "redirect:/nhan-vien/bai-dang";
        }

        // Đảo trạng thái
        post.setStatus(!post.getStatus());
        postService.save(post); // hoặc update nếu bạn đang dùng method riêng

        redirectAttributes.addFlashAttribute("successMessage",
                post.getStatus() ? "Bài đăng đã được hiển thị lại." : "Bài đăng đã bị ẩn.");
        return "redirect:/nhan-vien/bai-dang";
    }

    // Xóa bài đăng
    @PostMapping("/{postId}/delete")
    public String deletePost(@PathVariable Integer postId, RedirectAttributes redirectAttributes) {
        try {
            postService.deletePost(postId);
            redirectAttributes.addFlashAttribute("successMessage", "Bài đăng đã được xóa!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhan-vien/bai-dang";
    }

    // Chi tiết bài đăng
    @GetMapping("/chi-tiet-bai-dang/{postId}")
    public String showPostDetail(@PathVariable Integer postId, Model model, Principal principal) {

        Post post = postService.getPostById(postId);
        if (post == null) {
            return "redirect:/nhan-vien/bai-dang?error=Không tìm thấy bài đăng";
        }

        Set<Utility> utilities = new HashSet<>(postRepository.findUtilitiesByPostId(postId));

        log.info("Post {} utilities ({}): {}", postId, utilities.size(),
                utilities.stream().map(Utility::getName).collect(Collectors.toList()));

        List<String> images = post.getImages() != null && !post.getImages().isEmpty()
                ? post.getImages().stream().map(Image::getUrl).distinct().collect(Collectors.toList())
                : List.of("/images/cards/default.jpg");

        log.info("Post {} images: {}", postId, images);

        Hostel hostel = post.getHostel() != null
                ? hostelRepository.findByIdWithRooms(post.getHostel().getHostelId()).orElse(null)
                : null;

        List<Rooms> rooms = hostel != null && hostel.getRooms() != null ? hostel.getRooms() : List.of();
        Users author = post.getUser();
        model.addAttribute("author", author);
        model.addAttribute("images", images);
        model.addAttribute("post", post);
        model.addAttribute("utilities", utilities != null ? utilities : new HashSet<>());
        model.addAttribute("hostel", hostel);
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomCount", rooms.size());

        return "Staff/chi-tiet-bai-dang";
    }
}
