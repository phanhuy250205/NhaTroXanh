package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.CategoryService;
import nhatroxanh.com.Nhatroxanh.Service.PostService;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;
import nhatroxanh.com.Nhatroxanh.Model.entity.*;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @GetMapping
    public String showPostManagement(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String search,

            @RequestParam(defaultValue = "0") int pendingPage,
            @RequestParam(defaultValue = "0") int approvedPage,
            @RequestParam(defaultValue = "6") int size,

            Model model) {

        Sort sort = "oldest".equalsIgnoreCase(sortBy)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pendingPageable = PageRequest.of(pendingPage, size, sort);
        Pageable approvedPageable = PageRequest.of(approvedPage, size, sort);

        Page<Post> pendingPosts = postService.getFilteredPostsByApprovalStatus(
                ApprovalStatus.PENDING, type, search, pendingPageable);

        Page<Post> approvedPosts = postService.getFilteredPostsByApprovalStatus(
                ApprovalStatus.APPROVED, type, search, approvedPageable);

        model.addAttribute("pendingPosts", pendingPosts.getContent());
        model.addAttribute("approvedPosts", approvedPosts.getContent());
        model.addAttribute("pendingTotalPages", pendingPosts.getTotalPages());
        model.addAttribute("approvedTotalPages", approvedPosts.getTotalPages());
        model.addAttribute("pendingCurrentPage", pendingPage);
        model.addAttribute("approvedCurrentPage", approvedPage);

        model.addAttribute("categories", categoryService.getAllCategories());

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

        // Danh sách phòng
        List<Rooms> rooms = hostel != null && hostel.getRooms() != null ? hostel.getRooms() : List.of();

        // Debug
        log.info("Post {} utilities: {}", postId, utilities.stream().map(Utility::getName).toList());
        log.info("Rooms count: {}", rooms.size());
        rooms.forEach(room -> log.info("Room: id={}, name={}, price={}, area={}",
                room.getRoomId(), room.getNamerooms(), room.getPrice(), room.getAcreage()));
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

    @PostMapping("/{postId}/reject")
    public String rejectPost(@PathVariable Integer postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Bài đăng không tồn tại"));

            post.setApprovalStatus(ApprovalStatus.REJECTED);
            post.setApprovedBy(userDetails.getUser());
            postRepository.save(post);

            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối bài đăng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi từ chối bài đăng: " + e.getMessage());
        }
        return "redirect:/nhan-vien/bai-dang";
    }

}
