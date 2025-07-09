package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.Dto.VoucherDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.VoucherStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
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
public String showVoucherManagement(Model model,
        @RequestParam(defaultValue = "0") int activePage,
        @RequestParam(defaultValue = "0") int pendingPage,
        @RequestParam(defaultValue = "6") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String statusFilter,
        @RequestParam(required = false) String discountType,
        @RequestParam(required = false) String sortBy) {

    Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
    if ("oldest".equals(sortBy)) {
        sort = Sort.by(Sort.Direction.ASC, "createdAt");
    }

    Pageable activePageable = PageRequest.of(activePage, size, sort);
    Pageable pendingPageable = PageRequest.of(pendingPage, size, sort);

    Boolean status = null;
    Boolean discountTypeValue = null;

    if ("Hoạt động".equals(statusFilter)) {
        status = true;
    } else if ("Ngừng hoạt động".equals(statusFilter)) {
        status = false;
    }

    if ("Phần trăm".equals(discountType)) {
        discountTypeValue = true;
    } else if ("Số tiền cố định".equals(discountType)) {
        discountTypeValue = false;
    }

    Page<Vouchers> activeVouchers;
    Page<Vouchers> pendingVouchers;

    if (search != null && !search.trim().isEmpty()) {
        activeVouchers = voucherRepository.findBySearchAndFiltersWithStatus(
                search, status, discountTypeValue, VoucherStatus.APPROVED, activePageable);
        pendingVouchers = voucherRepository.findBySearchAndFiltersWithStatus(
                search, status, discountTypeValue, VoucherStatus.PENDING, pendingPageable);
    } else {
        activeVouchers = voucherRepository.findByFiltersWithStatus(
                status, discountTypeValue, VoucherStatus.APPROVED, activePageable);
        pendingVouchers = voucherRepository.findByFiltersWithStatus(
                status, discountTypeValue, VoucherStatus.PENDING, pendingPageable);
    }

    model.addAttribute("activeVouchers", activeVouchers);
    model.addAttribute("pendingVouchers", pendingVouchers);
    model.addAttribute("activePage", activePage);
    model.addAttribute("pendingPage", pendingPage);
    model.addAttribute("search", search);
    model.addAttribute("statusFilter", statusFilter);
    model.addAttribute("discountType", discountType);
    model.addAttribute("sortBy", sortBy);

    if (!model.containsAttribute("voucher")) {
        model.addAttribute("voucher", new Vouchers());
    }

    return "staff/khuyen-mai";
}


    @PostMapping("/tao")
    public String createVoucher(
            @ModelAttribute("voucher") Vouchers voucher,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            // Kiểm tra mã đã tồn tại
            if (voucherRepository.existsByCode(voucher.getCode())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mã voucher đã tồn tại!");
                redirectAttributes.addFlashAttribute("voucher", voucher); // giữ lại dữ liệu
                return "redirect:/nhan-vien/khuyen-mai";
            }

            Users user = userRepository.findById(userDetails.getUserId())
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            voucher.setUser(user);
            voucher.setCreatedAt(Date.valueOf(LocalDate.now()));
            voucher.setVoucherStatus(VoucherStatus.APPROVED);
            voucher.setHostel(null);
            voucher.setRoom(null);
            if (voucher.getQuantity() != null && voucher.getQuantity() == 0) {
                voucher.setStatus(false);
            }

            // ✅ Auto set status = false nếu endDate < hôm nay
            if (voucher.getEndDate() != null && voucher.getEndDate().before(Date.valueOf(LocalDate.now()))) {
                voucher.setStatus(false);
            }

            // ✅ Nếu không rơi vào 2 case trên, thì giữ nguyên status truyền từ form
            if (voucher.getStatus() == null) {
                voucher.setStatus(true); // mặc định là true nếu không set
            }
            voucherRepository.save(voucher);

            redirectAttributes.addFlashAttribute("successMessage", "Tạo voucher thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo voucher: " + e.getMessage());
            redirectAttributes.addFlashAttribute("voucher", voucher);
        }

        return "redirect:/nhan-vien/khuyen-mai";
    }

    @GetMapping("/tao-ma-tu-dong")
    public String generateVoucherCode(RedirectAttributes redirectAttributes) {
        String generatedCode = voucherService.generateUniqueVoucherCode();
        VoucherDTO voucherDTO = new VoucherDTO();
        voucherDTO.setCode(generatedCode);
        redirectAttributes.addFlashAttribute("voucherDTO", voucherDTO);
        return "redirect:/nhan-vien/khuyen-mai/tao";
    }

    @GetMapping("/cap-nhat/{voucherId}")
    public String showEditVoucherForm(@PathVariable Integer voucherId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        Vouchers voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        if (!voucher.getUser().getUserId().equals(userDetails.getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không có quyền chỉnh sửa");
            return "redirect:/nhan-vien/khuyen-mai";
        }

        model.addAttribute("voucher", voucher);
        return "staff/voucher-edit";
    }

    @PostMapping("/cap-nhat/{voucherId}")
    public String updateVoucher(
            @PathVariable Integer voucherId,
            @ModelAttribute("voucher") Vouchers updatedVoucher,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ!");
            return "redirect:/nhan-vien/khuyen-mai/cap-nhat/" + voucherId;
        }

        try {
            Vouchers existingVoucher = voucherRepository.findById(voucherId)
                    .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

            if (!existingVoucher.getUser().getUserId().equals(userDetails.getUserId())) {
                throw new RuntimeException("Bạn không có quyền chỉnh sửa voucher này!");
            }

            // Nếu mã được thay đổi thì kiểm tra trùng
            if (!existingVoucher.getCode().equals(updatedVoucher.getCode()) &&
                    voucherRepository.existsByCode(updatedVoucher.getCode())) {
                throw new RuntimeException("Mã voucher đã tồn tại!");
            }

            // Cập nhật dữ liệu
            existingVoucher.setCode(updatedVoucher.getCode());
            existingVoucher.setTitle(updatedVoucher.getTitle());
            existingVoucher.setDescription(updatedVoucher.getDescription());
            existingVoucher.setDiscountType(updatedVoucher.getDiscountType());
            existingVoucher.setDiscountValue(updatedVoucher.getDiscountValue());
            existingVoucher.setStartDate(updatedVoucher.getStartDate());
            existingVoucher.setEndDate(updatedVoucher.getEndDate());
            existingVoucher.setMinAmount(updatedVoucher.getMinAmount());
            existingVoucher.setQuantity(updatedVoucher.getQuantity());

            Date today = new Date(System.currentTimeMillis());

            boolean isExpired = updatedVoucher.getEndDate().before(today);
            boolean isOutOfQuantity = updatedVoucher.getQuantity() == 0;

            if (!isExpired && !isOutOfQuantity) {
                existingVoucher.setStatus(true);
            } else {
                existingVoucher.setStatus(false);
            }

            voucherRepository.save(existingVoucher);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/nhan-vien/khuyen-mai";
    }

    @PostMapping("/xoa/{voucherId}")
    public String deleteVoucher(@PathVariable Integer voucherId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteVoucher(voucherId, userDetails);
            redirectAttributes.addFlashAttribute("successMessage", "Voucher đã được xóa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhan-vien/khuyen-mai";
    }

    @PostMapping("/duyet/{voucherId}")
    public String approveVoucherTraditional(
            @PathVariable Integer voucherId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            voucherService.approveVoucher(voucherId, userDetails);
            redirectAttributes.addFlashAttribute("successMessage", "Voucher đã được duyệt thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/nhan-vien/khuyen-mai";
    }

    @PostMapping("/tu-choi/{voucherId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectVoucher(
            @PathVariable Integer voucherId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        try {
            voucherService.rejectVoucher(voucherId, userDetails);
            response.put("success", true);
            response.put("message", "Voucher đã bị từ chối!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // @GetMapping("/kiem-tra-ma")
    // @ResponseBody
    // public ResponseEntity<Map<String, Object>> checkVoucherCode(@RequestParam
    // String code) {
    // Map<String, Object> response = new HashMap<>();

    // boolean exists = voucherService.isVoucherCodeExists(code);
    // response.put("exists", exists);

    // if (exists) {
    // response.put("message", "Mã voucher đã tồn tại!");
    // } else {
    // response.put("message", "Mã voucher có thể sử dụng!");
    // }

    // return ResponseEntity.ok(response);
    // }

}
