package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Model.entity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.VoucherRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.VoucherService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/nhan-vien/khuyen-mai")
public class StaffVoucherController {

    @Autowired
    private VoucherService voucherService;
    @Autowired
    private VoucherRepository voucherRepository;
    @Autowired
    private UserRepository userRepository;

    @ModelAttribute("voucher")
    public Vouchers initVoucher() {
        Vouchers voucher = new Vouchers();
        voucher.setCode(voucherService.generateUniqueVoucherCode());
        voucher.setStatus(true);
        return voucher;
    }

    @GetMapping
    public String showVoucherList(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String sortBy) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("oldest".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Boolean status = null;

        if ("Hoạt động".equalsIgnoreCase(statusFilter)) {
            status = true;
        } else if ("Ngừng hoạt động".equalsIgnoreCase(statusFilter)) {
            status = false;
        }

        Page<Vouchers> vouchers;
        if (search != null && !search.trim().isEmpty()) {
            if (status != null) {
                vouchers = voucherRepository.searchVouchersByStatus(search, status, pageable);
            } else {
                vouchers = voucherRepository.searchVouchers(search, pageable);
            }
        } else {
            vouchers = (status != null)
                    ? voucherRepository.findByStatus(status, pageable)
                    : voucherRepository.findAll(pageable);
        }

        model.addAttribute("vouchers", vouchers);
        model.addAttribute("page", page);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("sortBy", sortBy);

        return "staff/khuyen-mai";
    }

    @PostMapping("/tao")
    public String createVoucher(@Valid @ModelAttribute("voucher") Vouchers voucher,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ!");
            redirect.addFlashAttribute("voucher", voucher);
            return "redirect:/nhan-vien/khuyen-mai";
        }

        try {
            if (voucherRepository.existsByCode(voucher.getCode())) {
                redirect.addFlashAttribute("errorMessage", "Mã voucher đã tồn tại!");
                redirect.addFlashAttribute("voucher", voucher);
                return "redirect:/nhan-vien/khuyen-mai";
            }

            Users user = userRepository.findById(userDetails.getUserId())
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));
            voucher.setUser(user);
            voucher.setCreatedAt(Date.valueOf(LocalDate.now()));

            if (voucher.getEndDate().before(new Date(System.currentTimeMillis())) || voucher.getQuantity() == 0) {
                voucher.setStatus(false);
            } else {
                voucher.setStatus(true);
            }

            voucherRepository.save(voucher);
            redirect.addFlashAttribute("successMessage", "Tạo voucher thành công!");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/nhan-vien/khuyen-mai";
    }

    @GetMapping("/cap-nhat/{id}")
    public String editVoucherForm(@PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirect) {
        Vouchers voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        if (!voucher.getUser().getUserId().equals(userDetails.getUserId())) {
            redirect.addFlashAttribute("errorMessage", "Không có quyền chỉnh sửa!");
            return "redirect:/nhan-vien/khuyen-mai";
        }

        model.addAttribute("voucher", voucher);
        return "staff/voucher-edit";
    }

    @PostMapping("/cap-nhat/{id}")
    public String updateVoucher(@PathVariable Integer id,
            @Valid @ModelAttribute("voucher") Vouchers updatedVoucher,
            BindingResult bindingResult,
            RedirectAttributes redirect,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ!");
            return "redirect:/nhan-vien/khuyen-mai/cap-nhat/" + id;
        }

        try {
            Vouchers voucher = voucherRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

            if (!voucher.getUser().getUserId().equals(userDetails.getUserId())) {
                throw new RuntimeException("Không có quyền chỉnh sửa!");
            }

            if (!voucher.getCode().equals(updatedVoucher.getCode())
                    && voucherRepository.existsByCode(updatedVoucher.getCode())) {
                throw new RuntimeException("Mã voucher đã tồn tại!");
            }

            voucher.setCode(updatedVoucher.getCode());
            voucher.setTitle(updatedVoucher.getTitle());
            voucher.setDescription(updatedVoucher.getDescription());
            voucher.setDiscountValue(updatedVoucher.getDiscountValue());
            voucher.setStartDate(updatedVoucher.getStartDate());
            voucher.setEndDate(updatedVoucher.getEndDate());
            voucher.setMinAmount(updatedVoucher.getMinAmount());
            voucher.setQuantity(updatedVoucher.getQuantity());

            Date today = new Date(System.currentTimeMillis());
            boolean expired = voucher.getEndDate().before(today);
            boolean out = voucher.getQuantity() == 0;
            voucher.setStatus(!expired && !out);

            voucherRepository.save(voucher);
            redirect.addFlashAttribute("successMessage", "Cập nhật thành công!");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/nhan-vien/khuyen-mai";
    }

    @PostMapping("/xoa/{id}")
    public String deleteVoucher(@PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirect) {
        try {
            voucherService.deleteVoucher(id, userDetails);
            redirect.addFlashAttribute("successMessage", "Đã xóa voucher!");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhan-vien/khuyen-mai";
    }

    @GetMapping("/tao-ma-tu-dong")
    @ResponseBody
    public Map<String, String> generateCode() {
        Map<String, String> response = new HashMap<>();
        response.put("code", voucherService.generateUniqueVoucherCode());
        return response;
    }
}
