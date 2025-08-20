package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Model.entity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Repository.VoucherRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.VoucherService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/chu-tro/voucher")
public class VoucherHostController {

    private static final Logger logger = LoggerFactory.getLogger(VoucherHostController.class);

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private HostelService hostelService;

    @Autowired
    private VoucherRepository voucherRepository;

    @GetMapping
    public String showVoucherManagement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "searchQuery", required = false) String searchQuery,
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            Model model) {
        Users currentUser = userDetails.getUser();
        List<Hostel> hostels = hostelService.getHostelsByOwnerId(currentUser.getUserId());

        int pageSize = 6;
        Pageable pageable = PageRequest.of(page, pageSize);

        String normalizedSearchQuery = (searchQuery != null && !searchQuery.trim().isEmpty()) ? searchQuery.trim()
                : null;
        String normalizedStatusFilter = (statusFilter != null && !statusFilter.trim().isEmpty()) ? statusFilter.trim()
                : null;

        Page<Vouchers> voucherPage = voucherService.getVouchersByOwnerIdWithFilters(
                currentUser.getUserId(), normalizedSearchQuery, normalizedStatusFilter, pageable);

        model.addAttribute("hostels", hostels);
        model.addAttribute("vouchers", voucherPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", voucherPage.getTotalPages());
        model.addAttribute("totalVouchers", voucherPage.getTotalElements());
        model.addAttribute("searchQuery", normalizedSearchQuery);
        model.addAttribute("statusFilter", normalizedStatusFilter);
        return "host/voucher-host";
    }

    @PostMapping("/create")
    public String createVoucher(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String title,
            @RequestParam String code,
            @RequestParam Integer hostelId,
            @RequestParam Float discountValue,
            @RequestParam Integer quantity,
            @RequestParam(required = false) Float minAmount,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            LocalDate maxEnd = start.plusMonths(2);

            if (end.isBefore(start)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ngày kết thúc không được trước ngày bắt đầu");
                return "redirect:/chu-tro/voucher";
            }
            if (end.isAfter(maxEnd)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Ngày kết thúc không được vượt quá 2 tháng kể từ ngày bắt đầu (" + maxEnd + ")");
                return "redirect:/chu-tro/voucher";
            }

            Users currentUser = userDetails.getUser();

            // Yêu cầu chọn khu trọ
            Hostel hostel = hostelService.getHostelById(hostelId)
                    .orElseThrow(() -> new IllegalArgumentException("Khu trọ không tồn tại."));

            if (!hostel.getOwner().getUserId().equals(currentUser.getUserId())) {
                throw new SecurityException("Bạn không có quyền tạo voucher cho khu trọ này.");
            }

            Vouchers voucher = Vouchers.builder()
                    .title(title.trim())
                    .code(code.trim())
                    .user(currentUser)
                    .hostel(hostel)
                    .discountValue(discountValue)
                    .quantity(quantity)
                    .minAmount(minAmount)
                    .startDate(Date.valueOf(start))
                    .endDate(Date.valueOf(end))
                    .description(description != null ? description.trim() : null)
                    .status(true)
                    .createdAt(Date.valueOf(LocalDate.now()))
                    .build();

            voucherService.createVoucherHost(voucher, currentUser.getUserId());

            redirectAttributes.addFlashAttribute("successMessage", "Tạo voucher thành công!");
        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi không xác định.");
        }

        return "redirect:/chu-tro/voucher";
    }

    @PostMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Integer voucherId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteVoucherByIdHost(voucherId, userDetails.getUser().getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Xóa voucher thành công!");
        } catch (IllegalArgumentException e) {
            logger.error("Lỗi khi xóa voucher: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (SecurityException e) {
            logger.error("Lỗi bảo mật khi xóa voucher: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xóa voucher này.");
        } catch (Exception e) {
            logger.error("Lỗi bất ngờ khi xóa voucher: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xóa voucher. Vui lòng thử lại.");
        }
        return "redirect:/chu-tro/voucher";
    }

    @GetMapping("/edit/{id}")
    public String showEditVoucherForm(@PathVariable("id") Integer voucherId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Vouchers voucher = voucherService.getVoucherByIdAndHost(voucherId, userDetails.getUser().getUserId());
            if (voucher.getHostel() == null) {
                throw new IllegalArgumentException("Voucher không hợp lệ: Không có khu trọ được liên kết.");
            }
            List<Hostel> hostels = hostelService.getHostelsByOwnerId(userDetails.getUser().getUserId());
            model.addAttribute("voucher", voucher);
            model.addAttribute("hostels", hostels);
            model.addAttribute("activeTab", "create");
            return "host/voucher-host";
        } catch (IllegalArgumentException e) {
            logger.error("Lỗi khi lấy voucher: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (SecurityException e) {
            logger.error("Lỗi bảo mật khi lấy voucher: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền chỉnh sửa voucher này.");
        } catch (Exception e) {
            logger.error("Lỗi bất ngờ khi lấy voucher: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tìm thấy voucher. Vui lòng thử lại.");
        }
        return "redirect:/chu-tro/voucher";
    }

    @PostMapping("/edit/{id}")
    public String updateVoucher(@PathVariable("id") Integer voucherId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("title") String title,
            @RequestParam("code") String code,
            @RequestParam("hostelId") Integer hostelId,
            @RequestParam("discountValue") Float discountValue,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "minAmount", required = false) Float minAmount,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("status") Boolean status,
            RedirectAttributes redirectAttributes) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            LocalDate maxEnd = start.plusMonths(2);

            if (end.isBefore(start)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ngày kết thúc không được trước ngày bắt đầu");
                return "redirect:/chu-tro/voucher/edit/" + voucherId;
            }
            if (end.isAfter(maxEnd)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Ngày kết thúc không được vượt quá 2 tháng kể từ ngày bắt đầu (" + maxEnd + ")");
                return "redirect:/chu-tro/voucher/edit/" + voucherId;
            }

            voucherService.updateVoucherHost(
                    voucherId, userDetails.getUser().getUserId(), title, code, hostelId,
                    discountValue, quantity, minAmount, Date.valueOf(start),
                    Date.valueOf(end), description, status);

            Vouchers voucher = voucherService.getVoucherByIdAndHost(voucherId, userDetails.getUser().getUserId());

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật voucher thành công!");
        } catch (SecurityException e) {
            logger.error("Lỗi bảo mật khi cập nhật voucher: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền chỉnh sửa voucher này.");
        } catch (Exception e) {
            logger.error("Lỗi bất ngờ khi cập nhật voucher: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật voucher.");
        }
        return "redirect:/chu-tro/voucher";
    }

    @GetMapping("/check-code")
    @ResponseBody
    public Map<String, Boolean> checkCode(@RequestParam String code,
            @RequestParam(required = false) Integer voucherId) {
        boolean exists = voucherId == null ? voucherService.existsByCode(code)
                : voucherService.existsByCodeAndNotId(code, voucherId);
        return Collections.singletonMap("exists", exists);
    }

    @PostMapping("/send-thong-bao/{id}")
    public String sendVoucherNotification(@PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirect) {
        try {
            Vouchers voucher = voucherRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

            if (!voucher.getUser().getUserId().equals(userDetails.getUserId())) {
                throw new RuntimeException("Không có quyền gửi thông báo cho voucher này!");
            }

            voucherService.sendVoucherNotification(voucher, userDetails);
            redirect.addFlashAttribute("successMessage", "Gửi thông báo voucher thành công!");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Lỗi khi gửi thông báo: " + e.getMessage());
        }
        return "redirect:/chu-tro/voucher";
    }
}