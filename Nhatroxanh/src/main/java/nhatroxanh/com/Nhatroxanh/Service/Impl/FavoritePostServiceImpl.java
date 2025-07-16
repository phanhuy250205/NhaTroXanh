package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.enity.FavoritePost;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.FavoritePostRepository;
import nhatroxanh.com.Nhatroxanh.Service.FavoritePostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class FavoritePostServiceImpl implements FavoritePostService {
    
    private static final Logger logger = LoggerFactory.getLogger(FavoritePostServiceImpl.class);
    
    @Autowired
    private FavoritePostRepository favoritePostRepository;
    
    @Override
    public void addToFavorites(Users user, Post post) {
        try {
            // Kiểm tra xem đã yêu thích chưa
            if (!favoritePostRepository.existsByUserAndPost(user, post)) {
                FavoritePost favoritePost = FavoritePost.builder()
                        .user(user)
                        .post(post)
                        .build();
                favoritePostRepository.save(favoritePost);
                logger.info("User {} added post {} to favorites", user.getUserId(), post.getPostId());
            } else {
                logger.warn("User {} already favorited post {}", user.getUserId(), post.getPostId());
            }
        } catch (Exception e) {
            logger.error("Error adding post {} to favorites for user {}: {}", 
                    post.getPostId(), user.getUserId(), e.getMessage());
            throw new RuntimeException("Không thể thêm vào danh sách yêu thích", e);
        }
    }
    
    @Override
    public void removeFromFavorites(Users user, Post post) {
        try {
            Optional<FavoritePost> favoritePost = favoritePostRepository.findByUserAndPost(user, post);
            if (favoritePost.isPresent()) {
                favoritePostRepository.delete(favoritePost.get());
                logger.info("User {} removed post {} from favorites", user.getUserId(), post.getPostId());
            } else {
                logger.warn("User {} has not favorited post {}", user.getUserId(), post.getPostId());
            }
        } catch (Exception e) {
            logger.error("Error removing post {} from favorites for user {}: {}", 
                    post.getPostId(), user.getUserId(), e.getMessage());
            throw new RuntimeException("Không thể xóa khỏi danh sách yêu thích", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isFavorited(Users user, Post post) {
        try {
            return favoritePostRepository.existsByUserAndPost(user, post);
        } catch (Exception e) {
            logger.error("Error checking favorite status for user {} and post {}: {}", 
                    user.getUserId(), post.getPostId(), e.getMessage());
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Post> getFavoritePostsByUser(Users user, Pageable pageable) {
        try {
            return favoritePostRepository.findFavoritePostsByUser(user, pageable);
        } catch (Exception e) {
            logger.error("Error getting favorite posts for user {}: {}", user.getUserId(), e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách yêu thích", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Post> getFavoritePostsByUser(Users user) {
        try {
            return favoritePostRepository.findFavoritePostsByUser(user);
        } catch (Exception e) {
            logger.error("Error getting favorite posts list for user {}: {}", user.getUserId(), e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách yêu thích", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countFavoritesByUser(Users user) {
        try {
            return favoritePostRepository.countByUser(user);
        } catch (Exception e) {
            logger.error("Error counting favorites for user {}: {}", user.getUserId(), e.getMessage());
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countFavoritesByPost(Post post) {
        try {
            return favoritePostRepository.countByPost(post);
        } catch (Exception e) {
            logger.error("Error counting favorites for post {}: {}", post.getPostId(), e.getMessage());
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Boolean> getFavoriteStatusForPosts(Users user, List<Post> posts) {
        Map<Integer, Boolean> favoriteStatus = new HashMap<>();
        try {
            for (Post post : posts) {
                // Chỉ kiểm tra trạng thái yêu thích nếu post chưa bị ẩn
                boolean isFavorited = post.getStatus() != null && post.getStatus() && 
                                    favoritePostRepository.existsByUserAndPost(user, post);
                favoriteStatus.put(post.getPostId(), isFavorited);
            }
        } catch (Exception e) {
            logger.error("Error getting favorite status for posts for user {}: {}", user.getUserId(), e.getMessage());
            // Trả về map rỗng nếu có lỗi
            for (Post post : posts) {
                favoriteStatus.put(post.getPostId(), false);
            }
        }
        return favoriteStatus;
    }
}
