package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

import nhatroxanh.com.Nhatroxanh.Model.enity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

public interface PostService {
        List<Post> findTopApprovedActivePostsByViews(int limit);

        List<Post> filterPosts(List<Integer> utilityIds, Float minArea, Float maxArea, String sort);

        List<Post> filterPostsByCategory(Integer categoryId, List<Integer> utilityIds, Float minArea, Float maxArea,
                        String sort);

        List<Post> getAllActivePosts();

        List<Post> getPostsByUserId(Integer userId);

        List<Post> searchPosts(String keyword, Integer categoryId, ApprovalStatus status, Date fromDate, Date toDate,
                        String sort);

        Post getPostById(Integer postId);

        void deletePost(Integer postId);

        void savePost(Post post);
          Post createPost(String title, String description, Float price, Float area,
                    Integer categoryId, Integer wardId, String street, String houseNumber,
                    List<Integer> utilityIds, MultipartFile[] images, Users user) throws Exception;

}
