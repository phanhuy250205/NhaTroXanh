package nhatroxanh.com.Nhatroxanh.Controller;

import nhatroxanh.com.Nhatroxanh.Model.entity.Post;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.FavoritePostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    
    private static final Logger logger = LoggerFactory.getLogger(FavoriteController.class);
    
    @Autowired
    private FavoritePostService favoritePostService;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Thêm/xóa post khỏi danh sách yêu thích
    @PostMapping("/toggle/{postId}")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable Integer postId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Kiểm tra authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để sử dụng tính năng này");
                response.put("requireLogin", true);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Lấy thông tin user
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Optional<Users> userOpt = userRepository.findById(userDetails.getUserId());
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin người dùng");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Lấy thông tin post
            Optional<Post> postOpt = postRepository.findById(postId);
            if (!postOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy bài đăng");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Users user = userOpt.get();
            Post post = postOpt.get();
            
            // Kiểm tra trạng thái hiện tại và toggle
            boolean currentlyFavorited = favoritePostService.isFavorited(user, post);
            
            if (currentlyFavorited) {
                favoritePostService.removeFromFavorites(user, post);
                response.put("action", "removed");
                response.put("message", "Đã xóa khỏi danh sách yêu thích");
            } else {
                favoritePostService.addToFavorites(user, post);
                response.put("action", "added");
                response.put("message", "Đã thêm vào danh sách yêu thích");
            }
            
            response.put("success", true);
            response.put("isFavorited", !currentlyFavorited);
            response.put("favoriteCount", favoritePostService.countFavoritesByPost(post));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error toggling favorite for post {}: {}", postId, e.getMessage());
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra. Vui lòng thử lại sau");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Kiểm tra trạng thái yêu thích của một post
    @GetMapping("/status/{postId}")
    public ResponseEntity<Map<String, Object>> getFavoriteStatus(@PathVariable Integer postId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                response.put("isFavorited", false);
                response.put("isAuthenticated", false);
                return ResponseEntity.ok(response);
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Optional<Users> userOpt = userRepository.findById(userDetails.getUserId());
            Optional<Post> postOpt = postRepository.findById(postId);
            
            if (!userOpt.isPresent() || !postOpt.isPresent()) {
                response.put("isFavorited", false);
                response.put("isAuthenticated", true);
                return ResponseEntity.ok(response);
            }
            
            boolean isFavorited = favoritePostService.isFavorited(userOpt.get(), postOpt.get());
            long favoriteCount = favoritePostService.countFavoritesByPost(postOpt.get());
            
            response.put("isFavorited", isFavorited);
            response.put("isAuthenticated", true);
            response.put("favoriteCount", favoriteCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting favorite status for post {}: {}", postId, e.getMessage());
            response.put("isFavorited", false);
            response.put("isAuthenticated", false);
            return ResponseEntity.ok(response);
        }
    }
    
    // Lấy danh sách post yêu thích của user hiện tại
    @GetMapping("/my-favorites")
    public ResponseEntity<Map<String, Object>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để xem danh sách yêu thích");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Optional<Users> userOpt = userRepository.findById(userDetails.getUserId());
            
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin người dùng");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Post> favoritePosts = favoritePostService.getFavoritePostsByUser(userOpt.get(), pageable);
            
            response.put("success", true);
            response.put("posts", favoritePosts.getContent());
            response.put("currentPage", favoritePosts.getNumber());
            response.put("totalPages", favoritePosts.getTotalPages());
            response.put("totalElements", favoritePosts.getTotalElements());
            response.put("hasNext", favoritePosts.hasNext());
            response.put("hasPrevious", favoritePosts.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting user favorites: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tải danh sách yêu thích");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Lấy trạng thái yêu thích cho nhiều post
    @PostMapping("/status/batch")
    public ResponseEntity<Map<String, Object>> getBatchFavoriteStatus(@RequestBody List<Integer> postIds) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                Map<Integer, Boolean> favoriteStatus = new HashMap<>();
                for (Integer postId : postIds) {
                    favoriteStatus.put(postId, false);
                }
                response.put("favoriteStatus", favoriteStatus);
                response.put("isAuthenticated", false);
                return ResponseEntity.ok(response);
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Optional<Users> userOpt = userRepository.findById(userDetails.getUserId());
            
            if (!userOpt.isPresent()) {
                Map<Integer, Boolean> favoriteStatus = new HashMap<>();
                for (Integer postId : postIds) {
                    favoriteStatus.put(postId, false);
                }
                response.put("favoriteStatus", favoriteStatus);
                response.put("isAuthenticated", false);
                return ResponseEntity.ok(response);
            }
            
            List<Post> posts = postRepository.findAllById(postIds);
            Map<Integer, Boolean> favoriteStatus = favoritePostService.getFavoriteStatusForPosts(userOpt.get(), posts);
            
            response.put("favoriteStatus", favoriteStatus);
            response.put("isAuthenticated", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting batch favorite status: {}", e.getMessage());
            Map<Integer, Boolean> favoriteStatus = new HashMap<>();
            for (Integer postId : postIds) {
                favoriteStatus.put(postId, false);
            }
            response.put("favoriteStatus", favoriteStatus);
            response.put("isAuthenticated", false);
            return ResponseEntity.ok(response);
        }
    }
}
