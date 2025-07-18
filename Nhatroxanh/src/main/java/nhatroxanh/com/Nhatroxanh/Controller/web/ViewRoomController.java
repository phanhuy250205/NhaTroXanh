package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
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
    public String showPostsByCategory(
            @PathVariable("id") Integer id,
            @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
            @RequestParam(value = "provinceCode", required = false) String provinceCode,
            @RequestParam(value = "districtCode", required = false) String districtCode,
            @RequestParam(value = "wardCode", required = false) String wardCode,
            @RequestParam(value = "priceRange", required = false) String priceRange,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        try {
            long startTime = System.currentTimeMillis();
            List<Province> provinces = provinceRepository.findAll();
            List<Utility> utilities = utilityRepository.findUtilitiesWithActivePosts();
            model.addAttribute("provinces", provinces != null ? provinces : new ArrayList<>());
            model.addAttribute("utilities", utilities != null ? utilities : new ArrayList<>());
            model.addAttribute("categoryId", id);

            Category category = categoryRepository.findById(id).orElse(null);
            int pageSize = 10;
            Float minPrice = null;
            Float maxPrice = null;

            // Parse price range
            if (priceRange != null && !priceRange.isEmpty()) {
                switch (priceRange.toLowerCase()) {
                    case "under_1m" -> maxPrice = 1_000_000f;
                    case "1_2m" -> {
                        minPrice = 1_000_000f;
                        maxPrice = 2_000_000f;
                    }
                    case "2_3m" -> {
                        minPrice = 2_000_000f;
                        maxPrice = 3_000_000f;
                    }
                    case "3_5m" -> {
                        minPrice = 3_000_000f;
                        maxPrice = 5_000_000f;
                    }
                    case "over_5m" -> minPrice = 5_000_000f;
                    default -> System.out.println("Invalid priceRange: " + priceRange);
                }
            }

            Sort pageSort = switch (sort.toLowerCase()) {
                case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
                case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
                default -> Sort.by(Sort.Direction.DESC, "createdAt");
            };
            Pageable pageable = PageRequest.of(page, pageSize, pageSort);

            Page<Post> postPage = postRepository.filterPostsWithAllConditions(
                    id, provinceCode, districtCode, wardCode,
                    null, // utilities chưa lọc ở giao diện này
                    null, null, // minArea, maxArea
                    minPrice, maxPrice,
                    searchTerm,
                    pageable);

            model.addAttribute("category", category);
            model.addAttribute("posts", postPage.getContent());
            model.addAttribute("totalPosts", postPage.getTotalElements());
            model.addAttribute("currentPage", postPage.getNumber());
            model.addAttribute("totalPages", postPage.getTotalPages());
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("selectedProvinceCode", provinceCode);
            model.addAttribute("selectedDistrictCode", districtCode);
            model.addAttribute("selectedWardCode", wardCode);
            model.addAttribute("selectedPriceRange", priceRange);
            model.addAttribute("searchTerm", searchTerm);
            model.addAttribute("selectedSort", sort);

            System.out.println("Posts fetched: " + postPage.getTotalElements() +
                    ", Time taken: " + (System.currentTimeMillis() - startTime) + "ms");

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("posts", new ArrayList<>());
            model.addAttribute("totalPosts", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("pageSize", 10);
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
    public ResponseEntity<?> testCategory(
            @PathVariable("categoryId") Integer categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "sort", defaultValue = "latest") String sort) {
        try {
            // Define page size
            int pageSize = 10;

            // Create Sort object based on sort parameter
            Sort pageSort;
            switch (sort.toLowerCase()) {
                case "price_asc":
                    pageSort = Sort.by(Sort.Direction.ASC, "price");
                    break;
                case "price_desc":
                    pageSort = Sort.by(Sort.Direction.DESC, "price");
                    break;
                case "latest":
                default:
                    pageSort = Sort.by(Sort.Direction.DESC, "createdAt");
                    break;
            }

            // Create Pageable object
            Pageable pageable = PageRequest.of(page, pageSize, pageSort);

            // Fetch paginated posts
            Page<Post> postPage = postRepository.findByCategoryIdAndStatusAndApprovalStatus(
                    categoryId, true, ApprovalStatus.APPROVED, pageable);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("categoryId", categoryId);
            response.put("message",
                    "Category " + categoryId + " has " + postPage.getTotalElements() + " approved posts");
            response.put("posts", postPage.getContent());
            response.put("totalPosts", postPage.getTotalElements());
            response.put("currentPage", postPage.getNumber());
            response.put("totalPages", postPage.getTotalPages());
            response.put("pageSize", pageSize);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // New method to handle all posts
    @GetMapping("/tat-ca-phong-tro")
    public String showAllPosts(
            @RequestParam(value = "provinceCode", required = false) String provinceCode,
            @RequestParam(value = "districtCode", required = false) String districtCode,
            @RequestParam(value = "wardCode", required = false) String wardCode,
            @RequestParam(value = "utilities", required = false) List<Integer> utilityIds,
            @RequestParam(value = "minArea", required = false) Float minArea,
            @RequestParam(value = "maxArea", required = false) Float maxArea,
            @RequestParam(value = "priceRange", required = false) String priceRange,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {
        try {
            long startTime = System.currentTimeMillis();

            List<Utility> utilities = utilityRepository.findUtilitiesWithActivePosts();
            List<Province> provinces = provinceRepository.findAll();
            if (utilities == null)
                utilities = new ArrayList<>();
            if (provinces == null)
                provinces = new ArrayList<>();

            model.addAttribute("utilities", utilities);
            model.addAttribute("provinces", provinces);

            Float minPrice = null;
            Float maxPrice = null;
            if (priceRange != null && !priceRange.isEmpty()) {
                switch (priceRange.toLowerCase()) {
                    case "under_1m" -> maxPrice = 1_000_000f;
                    case "1_2m" -> {
                        minPrice = 1_000_000f;
                        maxPrice = 2_000_000f;
                    }
                    case "2_3m" -> {
                        minPrice = 2_000_000f;
                        maxPrice = 3_000_000f;
                    }
                    case "3_5m" -> {
                        minPrice = 3_000_000f;
                        maxPrice = 5_000_000f;
                    }
                    case "over_5m" -> minPrice = 5_000_000f;
                }
            }

            Sort pageSort = switch (sort.toLowerCase()) {
                case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
                case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
                default -> Sort.by(Sort.Direction.DESC, "createdAt");
            };
            Pageable pageable = PageRequest.of(page, 10, pageSort);

            Page<Post> postPage = postRepository.filterPostsWithAllConditions(
                    null, provinceCode, districtCode, wardCode,
                    utilityIds, minArea, maxArea,
                    minPrice, maxPrice,
                    searchTerm,
                    pageable);

            model.addAttribute("posts", postPage.getContent());
            model.addAttribute("totalPosts", postPage.getTotalElements());
            model.addAttribute("currentPage", postPage.getNumber());
            model.addAttribute("totalPages", postPage.getTotalPages());
            model.addAttribute("pageSize", 10);

            // Gửi dữ liệu lọc lại về view
            model.addAttribute("selectedProvinceCode", provinceCode);
            model.addAttribute("selectedDistrictCode", districtCode);
            model.addAttribute("selectedWardCode", wardCode);
            model.addAttribute("selectedPriceRange", priceRange);
            model.addAttribute("selectedSort", sort);
            model.addAttribute("searchTerm", searchTerm);
            model.addAttribute("minArea", minArea);
            model.addAttribute("maxArea", maxArea);
            model.addAttribute("selectedUtilityIds", utilityIds);

            System.out.println("Fetched all posts: " + postPage.getTotalElements() + " in "
                    + (System.currentTimeMillis() - startTime) + "ms");

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("posts", new ArrayList<>());
            model.addAttribute("totalPosts", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("pageSize", 10);
            e.printStackTrace();
        }

        return "guest/tat-ca-phong";
    }

    @GetMapping("/api/filter-all-posts")
    @ResponseBody
    public ResponseEntity<?> filterAllPosts(
            @RequestParam(value = "utilities", required = false) List<Integer> utilityIds,
            @RequestParam(value = "minArea", required = false) Float minArea,
            @RequestParam(value = "maxArea", required = false) Float maxArea,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "provinceCode", required = false) String provinceCode,
            @RequestParam(value = "districtCode", required = false) String districtCode,
            @RequestParam(value = "wardCode", required = false) String wardCode,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "sort", defaultValue = "latest") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page) {

        try {
            int pageSize = 10;

            Sort pageSort = switch (sort.toLowerCase()) {
                case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
                case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
                default -> Sort.by(Sort.Direction.DESC, "createdAt");
            };

            Pageable pageable = PageRequest.of(page, pageSize, pageSort);

            Page<Post> postPage;
            if (utilityIds != null && !utilityIds.isEmpty()) {
                postPage = postRepository.filterPostsWithUtilitiesByCode(
                        utilityIds, minArea, maxArea, minPrice, maxPrice,
                        provinceCode, districtCode, wardCode, searchTerm, pageable); // ✅ thêm searchTerm
            } else {
                postPage = postRepository.filterPostsWithoutUtilitiesByCode(
                        minArea, maxArea, minPrice, maxPrice,
                        provinceCode, districtCode, wardCode, searchTerm, pageable); // ✅ thêm searchTerm
            }

            List<PostDTO> postDTOs = postMapper.toDTOList(postPage.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("content", postDTOs);
            response.put("number", postPage.getNumber());
            response.put("totalPages", postPage.getTotalPages());
            response.put("totalElements", postPage.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/api/filter-posts")
    @ResponseBody
    public ResponseEntity<?> filterPosts(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "provinceCode", required = false) String provinceCode,
            @RequestParam(value = "districtCode", required = false) String districtCode,
            @RequestParam(value = "wardCode", required = false) String wardCode,
            @RequestParam(value = "utilityIds", required = false) List<Integer> utilityIds,
            @RequestParam(value = "minArea", required = false) Float minArea,
            @RequestParam(value = "maxArea", required = false) Float maxArea,
            @RequestParam(value = "priceRange", required = false) String priceRange,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "sort", defaultValue = "latest") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page) {

        try {
            System.out.println("=== Filter Posts Request ===");
            System.out.println("CategoryId: " + categoryId);
            System.out.println(
                    "ProvinceCode: " + provinceCode + ", DistrictCode: " + districtCode + ", WardCode: " + wardCode);
            System.out.println("UtilityIds: " + utilityIds);
            System.out.println("MinArea: " + minArea + ", MaxArea: " + maxArea);
            System.out.println("PriceRange: " + priceRange + ", SearchTerm: " + searchTerm + ", Sort: " + sort
                    + ", Page: " + page);

            // Xử lý phân trang và sắp xếp
            int pageSize = 10;
            Sort pageSort = switch (sort.toLowerCase()) {
                case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
                case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
                default -> Sort.by(Sort.Direction.DESC, "createdAt");
            };
            Pageable pageable = PageRequest.of(page, pageSize, pageSort);

            // Xử lý khoảng giá
            Float minPrice = null;
            Float maxPrice = null;
            if (priceRange != null && !priceRange.isEmpty()) {
                switch (priceRange.toLowerCase()) {
                    case "under_1m" -> maxPrice = 1_000_000f;
                    case "1_2m" -> {
                        minPrice = 1_000_000f;
                        maxPrice = 2_000_000f;
                    }
                    case "2_3m" -> {
                        minPrice = 2_000_000f;
                        maxPrice = 3_000_000f;
                    }
                    case "3_5m" -> {
                        minPrice = 3_000_000f;
                        maxPrice = 5_000_000f;
                    }
                    case "over_5m" -> minPrice = 5_000_000f;
                    default -> System.out.println("Invalid priceRange: " + priceRange);
                }
            }

            // Gọi truy vấn duy nhất
            Page<Post> postPage = postRepository.filterPostsWithAllConditions(
                    categoryId, provinceCode, districtCode, wardCode,
                    utilityIds, minArea, maxArea,
                    minPrice, maxPrice, searchTerm, pageable);

            System.out.println("Filtered posts count: " + postPage.getTotalElements());

            List<PostDTO> postDTOs = postMapper.toDTOList(postPage.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("content", postDTOs);
            response.put("number", postPage.getNumber());
            response.put("totalPages", postPage.getTotalPages());
            response.put("totalElements", postPage.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Error filtering posts: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

}