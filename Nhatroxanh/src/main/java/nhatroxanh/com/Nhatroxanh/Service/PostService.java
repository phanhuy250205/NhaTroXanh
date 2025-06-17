package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;

    public List<Post> findTopApprovedActivePostsByViews(int limit) {
        return postRepository.findByStatusTrueAndApprovalStatusOrderByViewDesc(
                ApprovalStatus.APPROVED, PageRequest.of(0, limit)).getContent();
    }

    public List<Post> filterPosts(List<Integer> utilityIds, Float minArea, Float maxArea, String sort) {
        List<Post> posts;
        if (utilityIds != null && !utilityIds.isEmpty()) {
            posts = postRepository.findActivePostsWithUtilityFilter(utilityIds, minArea, maxArea);
        } else {
            posts = postRepository.findAllActivePostsWithAreaFilter(minArea, maxArea);
        }

        applySorting(posts, sort);

        return posts;
    }

    public List<Post> filterPostsByCategory(Integer categoryId, List<Integer> utilityIds, Float minArea, Float maxArea,
            String sort) {
        List<Post> posts;

        if (utilityIds != null && !utilityIds.isEmpty()) {
            posts = postRepository.findActiveCategoryPostsWithUtilityFilter(categoryId, utilityIds, minArea, maxArea);
        } else {
            posts = postRepository.findActiveCategoryPostsWithAreaFilter(categoryId, minArea, maxArea);
        }

        applySorting(posts, sort);

        return posts;
    }

    private void applySorting(List<Post> posts, String sort) {
        if (sort != null) {
            switch (sort) {
                case "price_asc":
                    posts.sort((a, b) -> Float.compare(a.getPrice(), b.getPrice()));
                    break;
                case "price_desc":
                    posts.sort((a, b) -> Float.compare(b.getPrice(), a.getPrice()));
                    break;
                case "latest":
                    posts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    break;
                default:
                    break;
            }
        }
    }

    public List<Post> getAllActivePosts() {
        return postRepository.findByStatusTrueAndApprovalStatusOrderByCreatedAtDesc("APPROVED");
    }
}
