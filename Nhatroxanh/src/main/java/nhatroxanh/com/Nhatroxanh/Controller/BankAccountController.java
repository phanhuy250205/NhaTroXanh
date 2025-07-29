package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.BankAccountService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/bank-account")
@RequiredArgsConstructor
@Slf4j
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final UserService userService;

    /**
     * Hiển thị trang quản lý tài khoản ngân hàng
     */
    @GetMapping("/manage")
    public String showBankAccountManagement(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập (chỉ ADMIN và STAFF)
            if (user.getRole() != Users.Role.ADMIN && user.getRole() != Users.Role.STAFF) {
                return "redirect:/access-denied";
            }

            // Lấy thông tin tài khoản ngân hàng hiện tại
            BankAccountService.BankAccountInfo currentBankInfo = bankAccountService.getStaffBankAccount();
            model.addAttribute("currentBankInfo", currentBankInfo);

            return "admin/bank-account-management";
        } catch (Exception e) {
            log.error("Error showing bank account management page: {}", e.getMessage());
            return "redirect:/admin/dashboard?error=system_error";
        }
    }

    /**
     * Cập nhật thông tin tài khoản ngân hàng cho nhân viên
     */
    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateBankAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("staffId") Integer staffId,
            @RequestParam("bankId") String bankId,
            @RequestParam("bankAccount") String bankAccount,
            @RequestParam("accountHolderName") String accountHolderName,
            @RequestParam("bankName") String bankName) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập (chỉ ADMIN)
            if (user.getRole() != Users.Role.ADMIN) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate input
            if (bankId == null || bankId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập mã ngân hàng");
                return ResponseEntity.badRequest().body(response);
            }

            if (bankAccount == null || bankAccount.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập số tài khoản");
                return ResponseEntity.badRequest().body(response);
            }

            if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập tên chủ tài khoản");
                return ResponseEntity.badRequest().body(response);
            }

            if (bankName == null || bankName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập tên ngân hàng");
                return ResponseEntity.badRequest().body(response);
            }

            // Cập nhật thông tin ngân hàng
            boolean success = bankAccountService.updateStaffBankAccount(
                staffId, bankId, bankAccount, accountHolderName, bankName);

            if (success) {
                response.put("success", true);
                response.put("message", "Cập nhật thông tin tài khoản ngân hàng thành công");
                
                log.info("Bank account updated successfully for staff {} by admin {}", 
                    staffId, user.getUserId());
            } else {
                response.put("success", false);
                response.put("message", "Không thể cập nhật thông tin tài khoản ngân hàng");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating bank account: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi cập nhật thông tin tài khoản ngân hàng");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy thông tin tài khoản ngân hàng hiện tại
     */
    @GetMapping("/current")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentBankAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users user = userDetails.getUser();
            
            // Kiểm tra quyền truy cập
            if (user.getRole() != Users.Role.ADMIN && user.getRole() != Users.Role.STAFF) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy thông tin tài khoản ngân hàng
            BankAccountService.BankAccountInfo bankInfo = bankAccountService.getStaffBankAccount();
            
            response.put("success", true);
            response.put("bankInfo", Map.of(
                "bankId", bankInfo.getBankId(),
                "accountNo", bankInfo.getAccountNo(),
                "accountHolderName", bankInfo.getAccountHolderName(),
                "bankName", bankInfo.getBankName()
            ));
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting current bank account: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Không thể lấy thông tin tài khoản ngân hàng");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
