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
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import nhatroxanh.com.Nhatroxanh.Service.VoucherService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/nhan-vien/khuyen-mai")
public class StaffVoucherController {
    private static final Logger logger = LoggerFactory.getLogger(StaffVoucherController.class);
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private VoucherRepository voucherRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;

    @ModelAttribute("voucher")
    public Vouchers initVoucher() {
        Vouchers voucher = new Vouchers();
        voucher.setCode(voucherService.generateUniqueVoucherCode());
        voucher.setStatus(true);
        return voucher;
    }

    @GetMapping
    public String showVoucherList(
            Model model,
            @AuthenticationPrincipal CustomUserDetails currentUserDetails, // <-- lấy người đăng nhập
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String sortBy) {

        // Lấy entity Users từ CustomUserDetails
        Users currentUser = currentUserDetails.getUser();

        // Thiết lập sắp xếp
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("oldest".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        // Xác định trạng thái nếu có filter
        Boolean status = null;
        if ("Hoạt động".equalsIgnoreCase(statusFilter)) {
            status = true;
        } else if ("Ngừng hoạt động".equalsIgnoreCase(statusFilter)) {
            status = false;
        }

        Page<Vouchers> vouchers;

        // Truy vấn dữ liệu phù hợp theo các trường hợp lọc
        if (search != null && !search.trim().isEmpty()) {
            if (status != null) {
                vouchers = voucherRepository.searchVouchersByStatus(currentUser, search.trim(), status, pageable);
            } else {
                vouchers = voucherRepository.searchVouchers(currentUser, search.trim(), pageable);
            }
        } else {
            if (status != null) {
                vouchers = voucherRepository.findByUserAndStatus(currentUser, status, pageable);
            } else {
                vouchers = voucherRepository.findByUser(currentUser, pageable);
            }
        }

        // Truyền dữ liệu ra view
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
        // Kiểm tra giá trị giảm giá không vượt quá 10% giá trị tối thiểu
        double maxDiscount = voucher.getMinAmount() * 0.1;
        if (voucher.getDiscountValue() > maxDiscount) {
            bindingResult.rejectValue("discountValue", "error.discountValue",
                    "Giá trị giảm giá không được vượt quá 10% giá trị tối thiểu (" + maxDiscount + " VNĐ).");
        }

        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Giá trị giảm giá không được vượt quá 10% giá trị tối thiểu");
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

            Date today = Date.valueOf(LocalDate.now());
            boolean isExpired = voucher.getEndDate() != null && voucher.getEndDate().before(today);
            boolean isOutOfStock = voucher.getQuantity() != null && voucher.getQuantity() == 0;
            if (isExpired || isOutOfStock) {
                voucher.setStatus(false);
                if (isExpired && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String reason = "Voucher đã hết hạn vào ngày " + voucher.getEndDate();
                    emailService.sendVoucherDeactivatedEmail(user.getEmail(), user.getFullname(), voucher.getTitle(),
                            reason);
                    logger.info("Đã gửi email thông báo hết hạn cho voucher {} đến {}", voucher.getCode(),
                            user.getEmail());
                } else if (isOutOfStock && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String reason = "Voucher đã hết số lượng";
                    emailService.sendVoucherDeactivatedEmail(user.getEmail(), user.getFullname(), voucher.getTitle(),
                            reason);
                    logger.info("Đã gửi email thông báo hết số lượng cho voucher {} đến {}", voucher.getCode(),
                            user.getEmail());
                }
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
        // Kiểm tra giá trị giảm giá không vượt quá 10% giá trị tối thiểu
        double maxDiscount = updatedVoucher.getMinAmount() * 0.1;
        if (updatedVoucher.getDiscountValue() > maxDiscount) {
            bindingResult.rejectValue("discountValue", "error.discountValue",
                    "Giá trị giảm giá không được vượt quá 10% giá trị tối thiểu (" + maxDiscount + " VNĐ).");
        }

        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Giá trị giảm giá không được vượt quá 10% giá trị tối thiểu");
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

            Date today = Date.valueOf(LocalDate.now());
            boolean isExpired = voucher.getEndDate() != null && voucher.getEndDate().before(today);
            boolean isOutOfStock = voucher.getQuantity() != null && voucher.getQuantity() == 0;
            boolean wasActive = voucher.getStatus();
            if (isExpired || isOutOfStock) {
                voucher.setStatus(false);
                if (wasActive && isExpired && voucher.getUser().getEmail() != null
                        && !voucher.getUser().getEmail().isEmpty()) {
                    String reason = "Voucher đã hết hạn vào ngày " + voucher.getEndDate();
                    emailService.sendVoucherDeactivatedEmail(voucher.getUser().getEmail(),
                            voucher.getUser().getFullname(), voucher.getTitle(), reason);
                    logger.info("Đã gửi email thông báo hết hạn cho voucher {} đến {}", voucher.getCode(),
                            voucher.getUser().getEmail());
                } else if (wasActive && isOutOfStock && voucher.getUser().getEmail() != null
                        && !voucher.getUser().getEmail().isEmpty()) {
                    String reason = "Voucher đã hết số lượng";
                    emailService.sendVoucherDeactivatedEmail(voucher.getUser().getEmail(),
                            voucher.getUser().getFullname(), voucher.getTitle(), reason);
                    logger.info("Đã gửi email thông báo hết số lượng cho voucher {} đến {}", voucher.getCode(),
                            voucher.getUser().getEmail());
                }
            } else {
                voucher.setStatus(true);
            }

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