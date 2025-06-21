package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Xử lý các yêu cầu đến trang chủ.
     * Ánh xạ các đường dẫn: "/", "/index", và "/trang-chu".
     * @param model Đối tượng Model để truyền dữ liệu đến view.
     * @return Tên của view (template) để hiển thị.
     */
    // ### THAY ĐỔI: Thêm "/index" và "/trang-chu" vào đây ###
    @GetMapping({"/", "/index", "/trang-chu"})
    public String home(Model model) {
        // Giữ nguyên logic lấy bài đăng của bạn
        List<Post> posts = postService.findTopApprovedActivePostsByViews(12);
        model.addAttribute("posts", posts);
        return "index";
    }

    /**
     * Xử lý yêu cầu xem chi tiết một bài đăng.
     * @param postId ID của bài đăng.
     * @param model Đối tượng Model.
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

            // Lấy các bài viết tương tự
            List<Post> similarPosts = postRepository.findSimilarPostsByCategory(postId,
                    post.getCategory() != null ? post.getCategory().getCategoryId() : null).stream()
                    .filter(p -> p.getStatus() && p.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .limit(4)
                    .collect(Collectors.toList());
            model.addAttribute("similarPosts", similarPosts);

            // Tăng lượt xem
            postRepository.incrementViewCount(postId);
            
            return "guest/chi-tiet";
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết bài đăng {}: {}", postId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi xảy ra. Vui lòng thử lại sau.");
            return "redirect:/error/500";
        }
    }
}
