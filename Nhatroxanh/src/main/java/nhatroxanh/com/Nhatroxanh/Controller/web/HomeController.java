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

    @GetMapping("/")
    public String home(Model model) {
        List<Post> posts = postService.findTopApprovedActivePostsByViews(12);
        model.addAttribute("posts", posts);
        return "index";
    }

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
            model.addAttribute("utilities", utilities != null ? utilities : new HashSet<>());

            // Hình ảnh
            List<String> images = post.getImages() != null && !post.getImages().isEmpty()
                    ? post.getImages().stream().map(Image::getUrl).distinct().collect(Collectors.toList())
                    : List.of("/images/cards/default.jpg");
            log.info("Post {} images: {}", postId, images);
            model.addAttribute("images", images);

            // Bài viết tương tự
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

}
