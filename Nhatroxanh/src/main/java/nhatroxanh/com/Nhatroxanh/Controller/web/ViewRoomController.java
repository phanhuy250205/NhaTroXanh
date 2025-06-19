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
import nhatroxanh.com.Nhatroxanh.Model.enity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Category;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Province;
import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Model.mapper.PostMapper;
import nhatroxanh.com.Nhatroxanh.Repository.CategoryRepository;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ProvinceRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;

@Controller
public class ViewRoomController {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private UtilityRepository utilityRepository;

    @GetMapping("/danh-muc/{id}")
    public String showPostsByCategory(@PathVariable("id") Integer id,
            @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
            @RequestParam(value = "province", required = false) Integer provinceId,
            @RequestParam(value = "priceRange", required = false) String priceRange,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            Model model) {
        try {
            System.out.println("Request: Category ID: " + id + ", Sort: " + sort + ", Province ID: " + provinceId +
                    ", Price Range: " + priceRange + ", Search Term: " + searchTerm);
            long startTime = System.currentTimeMillis();

            // Load provinces và utilities cho filter
            List<Province> provinces = provinceRepository.findAll();
            if (provinces == null) {
                provinces = new ArrayList<>();
                System.out.println("Warning: No provinces found in database");
            }

            List<Utility> utilities = utilityRepository.findUtilitiesWithActivePosts();
            if (utilities == null) {
                utilities = new ArrayList<>();
            }

            model.addAttribute("provinces", provinces);
            model.addAttribute("utilities", utilities);
            model.addAttribute("categoryId", id);

            Category category = categoryRepository.findById(id).orElse(null);
            List<Post> posts = new ArrayList<>();

            Float minPrice = null;
            Float maxPrice = null;

            // Xử lý khoảng giá
            if (priceRange != null && !priceRange.isEmpty()) {
                if (priceRange.startsWith("custom_")) {
                    String[] customRange = priceRange.replace("custom_", "").split("_");
                    if (customRange.length > 0 && !customRange[0].isEmpty()) {
                        minPrice = Float.parseFloat(customRange[0]);
                    }
                    if (customRange.length > 1 && !customRange[1].isEmpty()) {
                        maxPrice = Float.parseFloat(customRange[1]);
                    }
                } else {
                    switch (priceRange.toLowerCase()) {
                        case "under_1m":
                            maxPrice = 1_000_000f;
                            break;
                        case "1_2m":
                            minPrice = 1_000_000f;
                            maxPrice = 2_000_000f;
                            break;
                        case "2_3m":
                            minPrice = 2_000_000f;
                            maxPrice = 3_000_000f;
                            break;
                        case "3_5m":
                            minPrice = 3_000_000f;
                            maxPrice = 5_000_000f;
                            break;
                        case "over_5m":
                            minPrice = 5_000_000f;
                            break;
                        default:
                            System.out.println("Invalid priceRange: " + priceRange);
                    }
                }
            }

            if (category != null) {
                // Kiểm tra xem có filter nào được áp dụng không
                boolean hasFilters = (provinceId != null || priceRange != null ||
                        (searchTerm != null && !searchTerm.trim().isEmpty()));

                if (hasFilters) {
                    // Sử dụng các method có filter
                    switch (sort.toLowerCase()) {
                        case "price_asc":
                            posts = postRepository.findByCategoryAndProvinceAndPriceRangeAndSearchTermSortedByPriceAsc(
                                    id, provinceId, minPrice, maxPrice, searchTerm);
                            break;
                        case "price_desc":
                            posts = postRepository.findByCategoryAndProvinceAndPriceRangeAndSearchTermSortedByPriceDesc(
                                    id, provinceId, minPrice, maxPrice, searchTerm);
                            break;
                        case "latest":
                            posts = postRepository
                                    .findByCategoryAndProvinceAndPriceRangeAndSearchTermSortedByCreatedAtDesc(
                                            id, provinceId, minPrice, maxPrice, searchTerm);
                            break;
                        default:
                            posts = postRepository.findByCategoryAndProvinceAndPriceRangeAndSearchTerm(
                                    id, provinceId, minPrice, maxPrice, searchTerm);
                            break;
                    }
                } else {
                    // Không có filter, sử dụng method đơn giản với ApprovalStatus
                    posts = postRepository.findByCategoryIdAndStatusAndApprovalStatus(id, true,
                            ApprovalStatus.APPROVED);

                    // Sắp xếp theo yêu cầu
                    switch (sort.toLowerCase()) {
                        case "price_asc":
                            posts.sort((a, b) -> Float.compare(a.getPrice(), b.getPrice()));
                            break;
                        case "price_desc":
                            posts.sort((a, b) -> Float.compare(b.getPrice(), a.getPrice()));
                            break;
                        case "latest":
                        default:
                            posts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                            break;
                    }
                }

                System.out.println("Posts fetched: " + posts.size() + ", Time taken: " +
                        (System.currentTimeMillis() - startTime) + "ms");

                model.addAttribute("category", category);
                model.addAttribute("posts", posts);
                model.addAttribute("totalPosts", posts.size());
                model.addAttribute("selectedProvince", provinceId);
                model.addAttribute("selectedPriceRange", priceRange);
                model.addAttribute("searchTerm", searchTerm);
                model.addAttribute("selectedSort", sort);

                System.out.println("Category: " + category.getName() + ", Posts: " + posts.size());
            } else {
                model.addAttribute("category", null);
                model.addAttribute("posts", new ArrayList<>());
                model.addAttribute("totalPosts", 0);
                model.addAttribute("error", "Danh mục không tồn tại");
            }
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Khoảng giá không hợp lệ: " + e.getMessage());
            model.addAttribute("posts", new ArrayList<>());
            model.addAttribute("totalPosts", 0);
            System.out.println("NumberFormatException in showPostsByCategory: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("posts", new ArrayList<>());
            model.addAttribute("totalPosts", 0);
            System.out.println("Exception in showPostsByCategory: " + e.getMessage());
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

            List<Post> filteredPosts;

            // Sử dụng phương thức từ PostRepository để lọc bài đăng có status = true và
            // approvalStatus = 'APPROVED'
            if (utilityIds != null && !utilityIds.isEmpty()) {
                filteredPosts = postRepository.findActiveCategoryPostsWithUtilityFilter(categoryId, utilityIds, minArea,
                        maxArea);
            } else {
                filteredPosts = postRepository.findActiveCategoryPostsWithAreaFilter(categoryId, minArea, maxArea);
            }

            // Sắp xếp theo yêu cầu
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

    @GetMapping("/api/test-category/{categoryId}")
    @ResponseBody
    public ResponseEntity<?> testCategory(@PathVariable("categoryId") Integer categoryId) {
        try {
            // Sử dụng phương thức từ PostRepository để lọc bài đăng có status = true và
            // approvalStatus = 'APPROVED'
            List<Post> posts = postRepository.findByCategoryIdAndStatusAndApprovalStatus(categoryId, true,
                    ApprovalStatus.APPROVED);
            return ResponseEntity.ok("Category " + categoryId + " has " + posts.size() + " approved posts");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}