package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import nhatroxanh.com.Nhatroxanh.Model.enity.Category;
import nhatroxanh.com.Nhatroxanh.Repository.CategoryRepository;

@Controller
@RequestMapping("/nhan-vien/category")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepo;

    @GetMapping("")
    public String listAllCategory(Model model) {
        List<Category> categories = categoryRepo.findAll();
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", "");
        return "/staff/categoty";
    }

    @GetMapping("/search")
    public String searchCategory(Model model,
            @RequestParam(value = "keyword", required = false) String keyword) {
        List<Category> categories;
        if (keyword != null && !keyword.trim().isEmpty()) {
            categories = categoryRepo.findByNameContainingIgnoreCase(keyword);
        } else {
            categories = categoryRepo.findAll();
        }
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        return "/staff/categoty";
    }

    @PostMapping("/add")
    public String addCategory(@RequestParam("name") String name) {
        if (name != null && !name.trim().isEmpty()) {
            Category category = new Category();
            category.setName(name.trim());
            categoryRepo.save(category);
        }


        return "redirect:/quanly/category";

        return "redirect:/nhan-vien/category";

    }

    @PostMapping("/update")
    public String updateCategory(@RequestParam("id") Integer id,
            @RequestParam("name") String name) {
        Category category = categoryRepo.findById(id).orElse(null);
        if (category != null && name != null && !name.trim().isEmpty()) {
            category.setName(name.trim());
            categoryRepo.save(category);
        }
        return "redirect:/nhan-vien/category";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id) {
        if (categoryRepo.existsById(id)) {
            categoryRepo.deleteById(id);
        }
        return "redirect:/nhan-vien/category";
    }
}
