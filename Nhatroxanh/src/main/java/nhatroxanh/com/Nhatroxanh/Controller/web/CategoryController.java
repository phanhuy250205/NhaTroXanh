package nhatroxanh.com.Nhatroxanh.Controller.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.entity.Category;
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
        return "staff/categoty";
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
    public String addCategory(@RequestParam("name") String name, RedirectAttributes redirect) {
        if (name != null && !name.trim().isEmpty()) {
            Category category = new Category();
            category.setName(name.trim());
            categoryRepo.save(category);
            redirect.addFlashAttribute("successMessage", "Thêm danh mục thành công!");
        } else {
            redirect.addFlashAttribute("errorMessage", "Tên danh mục không được để trống!");
        }
        return "redirect:/nhan-vien/category";
    }

    @PostMapping("/update")
    public String updateCategory(@RequestParam("id") Integer id,
            @RequestParam("name") String name,
            RedirectAttributes redirect) {
        Category category = categoryRepo.findById(id).orElse(null);
        if (category != null && name != null && !name.trim().isEmpty()) {
            category.setName(name.trim());
            categoryRepo.save(category);
            redirect.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
        } else {
            redirect.addFlashAttribute("errorMessage", "Cập nhật thất bại. Dữ liệu không hợp lệ!");
        }
        return "redirect:/nhan-vien/category";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes redirect) {
        if (categoryRepo.existsById(id)) {
            categoryRepo.deleteById(id);
            redirect.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
        } else {
            redirect.addFlashAttribute("errorMessage", "Không tìm thấy danh mục để xóa!");
        }
        return "redirect:/nhan-vien/category";
    }

}
