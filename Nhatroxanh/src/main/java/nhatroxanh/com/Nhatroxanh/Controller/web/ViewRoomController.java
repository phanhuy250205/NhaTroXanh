package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import nhatroxanh.com.Nhatroxanh.Model.Dto.PostDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Category;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Model.mapper.PostMapper;
import nhatroxanh.com.Nhatroxanh.Repository.CategoryRepository;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;

@Controller
public class ViewRoomController {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UtilityRepository utilityRepository;

    @Autowired
    private PostMapper postMapper;

    @GetMapping("/danh-muc/{id}")
    public String showPostsByCategory(@PathVariable("id") Integer id,
            @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
            Model model) {
        try {
            Category category = categoryRepository.findById(id).orElse(null);
            List<Post> posts = new ArrayList<>();

            List<Utility> utilities = utilityRepository.findUtilitiesWithActivePosts();

            if (category != null) {
                posts = postRepository.findAllByCategoryId(id);

                model.addAttribute("category", category);
                model.addAttribute("posts", posts);
                model.addAttribute("utilities", utilities);
                model.addAttribute("totalPosts", posts.size());
                model.addAttribute("categoryId", id);

                System.out.println("Category: " + category.getName() + ", Posts: " + posts.size());
            } else {
                model.addAttribute("category", null);
                model.addAttribute("posts", new ArrayList<>());
                model.addAttribute("utilities", utilities);
                model.addAttribute("error", "Danh mục không tồn tại");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu");
            model.addAttribute("posts", new ArrayList<>());
            model.addAttribute("utilities", new ArrayList<>());
            System.out.println("Error in showPostsByCategory: " + e.getMessage());
            e.printStackTrace();
        }

        return "guest/phong-tro";
    }

    @GetMapping("/api/filter-posts-by-category/{categoryId}")
    @ResponseBody
    public ResponseEntity<?> filterPostsByCategory(
            @PathVariable("categoryId") Integer categoryId,
            @RequestParam(value = "utilities", required = false) List<Integer> utilityIds,
            @RequestParam(value = "minArea", required = false) Float minArea,
            @RequestParam(value = "maxArea", required = false) Float maxArea,
            @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort) {

        try {
            System.out.println("=== Filter Request ===");
            System.out.println("CategoryId: " + categoryId);
            System.out.println("UtilityIds: " + utilityIds);
            System.out.println("MinArea: " + minArea + ", MaxArea: " + maxArea);
            System.out.println("Sort: " + sort);

            List<Post> filteredPosts = new ArrayList<>();

            List<Post> allCategoryPosts = postRepository.findAllByCategoryId(categoryId);
            System.out.println("All category posts: " + allCategoryPosts.size());

            for (Post post : allCategoryPosts) {
                boolean matchesFilter = true;

                if (minArea != null && post.getArea() < minArea) {
                    matchesFilter = false;
                }
                if (maxArea != null && post.getArea() > maxArea) {
                    matchesFilter = false;
                }

                if (utilityIds != null && !utilityIds.isEmpty()) {
                    boolean hasUtility = false;
                    if (post.getUtilities() != null) {
                        for (Integer utilityId : utilityIds) {
                            if (post.getUtilities().stream().anyMatch(u -> u.getUtilityId().equals(utilityId))) {
                                hasUtility = true;
                                break;
                            }
                        }
                    }
                    if (!hasUtility) {
                        matchesFilter = false;
                    }
                }

                if (matchesFilter) {
                    filteredPosts.add(post);
                }
            }

            switch (sort) {
                case "price_asc":
                    filteredPosts.sort((a, b) -> Float.compare(a.getPrice(), b.getPrice()));
                    break;
                case "price_desc":
                    filteredPosts.sort((a, b) -> Float.compare(b.getPrice(), a.getPrice()));
                    break;
                case "latest":
                    filteredPosts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    break;
            }

            System.out.println("Filtered posts count: " + filteredPosts.size());

            List<PostDTO> postDTOs = postMapper.toDTOList(filteredPosts);

            return ResponseEntity.ok(postDTOs);

        } catch (Exception e) {
            System.out.println("Error filtering posts by category: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // Test endpoint to check if API is working
    @GetMapping("/api/test-category/{categoryId}")
    @ResponseBody
    public ResponseEntity<?> testCategory(@PathVariable("categoryId") Integer categoryId) {
        try {
            List<Post> posts = postRepository.findAllByCategoryId(categoryId);
            return ResponseEntity.ok("Category " + categoryId + " has " + posts.size() + " posts");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
