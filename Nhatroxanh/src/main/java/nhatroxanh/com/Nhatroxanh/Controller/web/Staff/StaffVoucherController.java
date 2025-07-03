package nhatroxanh.com.Nhatroxanh.Controller.web.Staff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.VoucherService;

@Controller
@RequestMapping("/nhan-vien/khuyen-mai")
public class StaffVoucherController {
    @Autowired
    private VoucherService voucherService;

    @GetMapping
    public String showVoucherManagement(Model model,
            @RequestParam(defaultValue = "0") int activePage,
            @RequestParam(defaultValue = "0") int pendingPage,
            @RequestParam(defaultValue = "6") int size) {

        Pageable activePageable = PageRequest.of(activePage, size);
        Pageable pendingPageable = PageRequest.of(pendingPage, size);

        Page<Vouchers> activeVouchers = voucherService.getActiveVouchers(activePageable);
        Page<Vouchers> pendingVouchers = voucherService.getPendingVouchers(pendingPageable);

        Vouchers voucher = new Vouchers();
        voucher.setFormattedDiscount(voucher.getFormattedDiscount());
        model.addAttribute("formattedDiscount", voucher.getFormattedDiscount());

        model.addAttribute("activeVouchers", activeVouchers);
        model.addAttribute("pendingVouchers", pendingVouchers);
        model.addAttribute("activePage", activePage);
        model.addAttribute("pendingPage", pendingPage);

        return "staff/khuyen-mai";
    }

    @PostMapping("/xoa/{voucherId}")
    public String deleteVoucher(@PathVariable Integer voucherId, CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteVoucher(voucherId, userDetails);
            redirectAttributes.addFlashAttribute("successMessage", "Voucher đã được xóa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhan-vien/khuyen-mai";
    }
}
