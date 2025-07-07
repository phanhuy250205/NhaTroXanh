package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface FavoritePostService {
    
    // Thêm post vào danh sách yêu thích
    void addToFavorites(Users user, Post post);
    
    // Xóa post khỏi danh sách yêu thích
    void removeFromFavorites(Users user, Post post);
    
    // Kiểm tra xem post có được yêu thích bởi user không
    boolean isFavorited(Users user, Post post);
    
    // Lấy danh sách post yêu thích của user (có phân trang)
    Page<Post> getFavoritePostsByUser(Users user, Pageable pageable);
    
    // Lấy danh sách post yêu thích của user
    List<Post> getFavoritePostsByUser(Users user);
    
    // Đếm số lượng post yêu thích của user
    long countFavoritesByUser(Users user);
    
    // Đếm số lượng user yêu thích post
    long countFavoritesByPost(Post post);
    
    // Lấy trạng thái yêu thích của nhiều post cho một user
    Map<Integer, Boolean> getFavoriteStatusForPosts(Users user, List<Post> posts);
}
