package nhatroxanh.com.Nhatroxanh.Controller.web.Customer;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.validator.internal.util.Contracts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentResponseDto;

import nhatroxanh.com.Nhatroxanh.Model.entity.DetailPayments;
import nhatroxanh.com.Nhatroxanh.Model.entity.Payments;
import nhatroxanh.com.Nhatroxanh.Service.PaymentService;

@Controller
public class PaymentCustomerController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping("/khach-thue/lich-su-thanh-toan")
    public String lichsuthanhtoan(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "method", required = false) String method,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model) {

        // Fetch paginated payment history
        Page<Payments> paymentPage = paymentService.getPaymentHistory(year, status, method, page, size);

        // Format dates for display
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        paymentPage.getContent().forEach(payment -> {
            payment.setPaymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate() : null);
            payment.setDueDate(payment.getDueDate() != null ? payment.getDueDate() : null);
        });

        // Add data to model
        model.addAttribute("payments", paymentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paymentPage.getTotalPages());
        model.addAttribute("totalItems", paymentPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("currentYear", year != null ? year : "");
        model.addAttribute("currentStatus", status != null ? status : "Tất cả");
        model.addAttribute("currentMethod", method != null ? method : "Tất cả");

        return "guest/lichsu-thanhtoan";
    }

    @GetMapping("/khach-thue/invoice/{paymentId}")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable Integer paymentId) {
        try {
            Payments payment = paymentService.findPaymentById(paymentId);
            List<DetailPayments> items = paymentService.getDetailPaymentsByPaymentId(paymentId);

            Map<String, Object> response = new HashMap<>();
            response.put("payment", new PaymentResponse(
                    payment.getId(),
                    payment.getTotalAmount(),
                    payment.getDueDate(),
                    payment.getPaymentDate(),
                    payment.getPaymentStatus() != null ? payment.getPaymentStatus().toString() : null,
                    payment.getPaymentMethod() != null ? payment.getPaymentMethod().toString() : null));
            response.put("items", items.stream().map(item -> new DetailPaymentResponse(
                    item.getDetailId(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getAmountUnitPrice())).toList());

            nhatroxanh.com.Nhatroxanh.Model.entity.Contracts contract = payment.getContract();
            if (contract == null) {
                throw new RuntimeException("Hợp đồng không tồn tại cho hóa đơn ID: " + paymentId);
            }
            nhatroxanh.com.Nhatroxanh.Model.entity.Rooms room = contract.getRoom();
            if (room == null || room.getHostel() == null) {
                throw new RuntimeException("Phòng hoặc nhà trọ không tồn tại cho hóa đơn ID: " + paymentId);
            }
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomCode", room.getNamerooms() != null ? room.getNamerooms() : "Không xác định");
            roomInfo.put("hostelName",
                    room.getHostel().getName() != null ? room.getHostel().getName() : "Không xác định");
            roomInfo.put("tenantName",
                    contract.getTenant() != null ? contract.getTenant().getFullname()
                            : contract.getUnregisteredTenant() != null ? contract.getUnregisteredTenant().getFullName()
                                    : "Không xác định");
            roomInfo.put("tenantPhone", contract.getTenantPhone() != null ? contract.getTenantPhone() : "-");
            response.put("roomInfo", roomInfo);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi server: Không thể tải chi tiết hóa đơn: " + e.getMessage());
        }
    }

    // Record để serialize chỉ các trường cần thiết từ Payments
    private record PaymentResponse(
            Integer id,
            Float totalAmount,
            java.sql.Date dueDate,
            java.sql.Date paymentDate,
            String paymentStatus,
            String paymentMethod) {
    }

    // Record để serialize chỉ các trường cần thiết từ DetailPayments
    private record DetailPaymentResponse(
            Integer detailId,
            String itemName,
            Integer quantity,
            Float unitPrice,
            Float amountUnitPrice) {
    }
}