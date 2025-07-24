package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import nhatroxanh.com.Nhatroxanh.Service.UtilityService;

@Controller
@RequestMapping("/nhan-vien/utility")
public class UltilityController {
    @Autowired
    private UtilityService utilityService;

    @GetMapping
    public String listUtilities(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("utilities", utilityService.searchUtilities(keyword));
        model.addAttribute("keyword", keyword);
        return "staff/utility";
    }

    @PostMapping("/add")
    public String addUtility(@ModelAttribute Utility utility, RedirectAttributes redirectAttributes) {
        try {
            utilityService.addUtility(utility);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm tiện ích thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhan-vien/utility";
    }

    @PostMapping("/update")
    public String updateUtility(@RequestParam Integer id, @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        try {
            utilityService.updateUtility(id, name);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tiện ích thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhan-vien/utility";
    }

    @GetMapping("/delete/{id}")
    public String deleteUtility(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            utilityService.deleteUtilitystaff(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tiện ích thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhan-vien/utility";
    }

    @GetMapping("/search")
    public String searchUtilities(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("utilities", utilityService.searchUtilities(keyword));
        model.addAttribute("keyword", keyword);
        return "staff/utility";
    }
}
