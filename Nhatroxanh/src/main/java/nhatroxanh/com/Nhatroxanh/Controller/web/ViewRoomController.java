package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import nhatroxanh.com.Nhatroxanh.Model.enity.Category;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Repository.CategoryRepository;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;

@Controller
public class ViewRoomController {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @GetMapping("/danh-muc/{id}")
    public String showPostsByCategory(@PathVariable("id") Integer id,
            @RequestParam(value = "sort", required = false, defaultValue = "all") String sort,
            Model model) {
        try {
            Category category = categoryRepository.findById(id).orElse(null);
            List<Post> posts = new ArrayList<>();

            if (category != null) {
                switch (sort.toLowerCase()) {
                    case "price_asc":
                        posts = postRepository.findByCategorySortedByPriceAsc(id);
                        System.out.println("Sorted by price ascending, size: " + posts.size());
                        break;
                    case "price_desc":
                        posts = postRepository.findByCategorySortedByPriceDesc(id);
                        System.out.println("Sorted by price descending, size: " + posts.size());
                        break;
                    case "latest":
                        posts = postRepository.findByCategorySortedByCreatedAtDesc(id);
                        System.out.println("Sorted by latest, size: " + posts.size());
                        break;
                    case "all":
                    default:
                        posts = postRepository.findAllByCategoryId(id);
                        System.out.println("All posts, size: " + posts.size());
                        break;
                }
                model.addAttribute("category", category);
                model.addAttribute("posts", posts);
                model.addAttribute("totalPosts", posts.size());
            } else {
                model.addAttribute("category", null);
                model.addAttribute("posts", new ArrayList<>());
                model.addAttribute("error", "Danh mục không tồn tại");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu");
            model.addAttribute("posts", new ArrayList<>());
            System.out.println("Error in showPostsByCategory: " + e.getMessage());
            e.printStackTrace();
        }

        return "guest/phong-tro";
    }

}
