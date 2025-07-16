package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import nhatroxanh.com.Nhatroxanh.Model.enity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

public interface PostService {
        List<Post> findTopApprovedActivePostsByViews(int limit);

        List<Post> filterPosts(List<Integer> utilityIds, Float minArea, Float maxArea, String sort);

        List<Post> filterPostsByCategory(Integer categoryId, List<Integer> utilityIds, Float minArea, Float maxArea,
                        String sort);

        List<Post> getAllActivePosts();

        Page<Post> getPostsByUserId(Integer userId, Pageable pageable);

        Page<Post> searchPosts(String keyword, Integer categoryId, ApprovalStatus status,
                        Date fromDate, Date toDate, String sort, Integer userId, Pageable pageable);

        Post getPostById(Integer postId);

        void deletePost(Integer postId);

        void savePost(Post post);

        Post createPost(String title, String description, Float price, Float area,
                        Integer categoryId, String wardCode, String street, String houseNumber,
                        List<Integer> utilityIds, MultipartFile[] images, Integer hostelId, Users user,
                        String provinceCode, String districtCode, String provinceName,
                        String districtName, String wardName) throws Exception;

        Post updatePost(Integer postId, String title, String description, Float price, Float area,
                        Integer categoryId, String wardCode, String street, String houseNumber,
                        List<Integer> utilityIds, MultipartFile[] images, List<Integer> imagesToDelete,
                        List<Integer> imagesToKeep, Integer hostelId, Users user, String provinceCode,
                        String districtCode,
                        String provinceName, String districtName, String wardName) throws Exception;

        void save(Post post);

        void approvePost(Integer postId, Users approvedBy);

        void hidePost(Integer postId);

        List<Post> getFilteredPosts(String status, String type, String sortBy, String search);

        Page<Post> getFilteredPostsByApprovalStatus(
        ApprovalStatus approvalStatus,
        String type,
        String search,
        Pageable pageable);

}
