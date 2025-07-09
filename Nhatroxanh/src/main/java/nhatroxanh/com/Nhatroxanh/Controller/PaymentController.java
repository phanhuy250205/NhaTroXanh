package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentRequestDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentResponseDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments.PaymentMethod;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments.PaymentStatus;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.PaymentService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Maps English status values from frontend to Vietnamese enum constants
     */
    private PaymentStatus mapStatusFromString(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }

        switch (status.toLowerCase()) {
            case "paid":
                return PaymentStatus.ĐÃ_THANH_TOÁN;
            case "unpaid":
                return PaymentStatus.CHƯA_THANH_TOÁN;
            case "overdue":
                return PaymentStatus.QUÁ_HẠN_THANH_TOÁN;
            default:
                // Try to match Vietnamese enum constants directly
                try {
                    return PaymentStatus.valueOf(status);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown payment status: {}", status);
                    return null;
                }
        }
    }

    /**
     * Lấy tất cả payments của owner hiện tại
     */
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<PaymentResponseDto>> getPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            List<PaymentResponseDto> payments;

            if (search != null && !search.trim().isEmpty()) {
                log.info("Searching payments for owner {} with keyword: '{}'", ownerId, search.trim());
                payments = paymentService.searchPayments(ownerId, search.trim());
                log.info("Found {} payments for search keyword: '{}'", payments.size(), search.trim());
            } else if (status != null && !status.isEmpty()) {
                PaymentStatus paymentStatus = mapStatusFromString(status);
                if (paymentStatus != null) {
                    log.info("Filtering payments for owner {} with status: {}", ownerId, paymentStatus);
                    payments = paymentService.getPaymentsByOwnerIdAndStatus(ownerId, paymentStatus);
                } else {
                    log.warn("Invalid payment status: {}, returning all payments", status);
                    payments = paymentService.getPaymentsByOwnerId(ownerId);
                }
            } else {
                payments = paymentService.getPaymentsByOwnerId(ownerId);
            }

            return ResponseEntity.ok(payments);

        } catch (Exception e) {
            log.error("Error getting payments: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy payments với phân trang
     */
    @GetMapping("/paginated")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            Pageable pageable = PageRequest.of(page, size);
            Page<PaymentResponseDto> paymentsPage;

            if (search != null && !search.trim().isEmpty()) {
                log.info("Searching payments with pagination for owner {} with keyword: '{}'", ownerId, search.trim());
                paymentsPage = paymentService.searchPaymentsWithPagination(ownerId, search.trim(), pageable);
                log.info("Found {} payments (page {}) for search keyword: '{}'", paymentsPage.getContent().size(), page,
                        search.trim());
            } else if (status != null && !status.isEmpty()) {
                PaymentStatus paymentStatus = mapStatusFromString(status);
                if (paymentStatus != null) {
                    log.info("Filtering payments with pagination for owner {} with status: {}", ownerId, paymentStatus);
                    paymentsPage = paymentService.getPaymentsByOwnerIdAndStatusWithPagination(ownerId, paymentStatus,
                            pageable);
                } else {
                    log.warn("Invalid payment status: {}, returning all payments", status);
                    paymentsPage = paymentService.getPaymentsByOwnerIdWithPagination(ownerId, pageable);
                }
            } else {
                paymentsPage = paymentService.getPaymentsByOwnerIdWithPagination(ownerId, pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("payments", paymentsPage.getContent());
            response.put("currentPage", paymentsPage.getNumber());
            response.put("totalPages", paymentsPage.getTotalPages());
            response.put("totalElements", paymentsPage.getTotalElements());
            response.put("pageSize", paymentsPage.getSize());
            response.put("hasNext", paymentsPage.hasNext());
            response.put("hasPrevious", paymentsPage.hasPrevious());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting paginated payments: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy 8 payments mới nhất
     */
    @GetMapping("/recent")
    @ResponseBody
    public ResponseEntity<List<PaymentResponseDto>> getRecentPayments(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            List<PaymentResponseDto> recentPayments = paymentService.getRecentPaymentsByOwnerId(ownerId);
            return ResponseEntity.ok(recentPayments);

        } catch (Exception e) {
            log.error("Error getting recent payments: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy chi tiết payment theo ID
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<PaymentResponseDto> getPaymentById(@PathVariable Integer id) {
        try {
            PaymentResponseDto payment = paymentService.getPaymentById(id);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Error getting payment by id: ", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Tạo payment/invoice mới
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestBody PaymentRequestDto request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Có thể thêm validation owner ở đây nếu cần

            PaymentResponseDto payment = paymentService.createPayment(request);

            response.put("success", true);
            response.put("message", "Tạo hóa đơn thành công");
            response.put("data", payment);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating payment: ", e);
            response.put("success", false);
            response.put("message", "Lỗi tạo hóa đơn: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cập nhật trạng thái payment
     */
    @PutMapping("/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            String statusStr = request.get("status");
            PaymentStatus status = mapStatusFromString(statusStr);

            if (status == null) {
                response.put("success", false);
                response.put("message", "Trạng thái không hợp lệ: " + statusStr);
                return ResponseEntity.badRequest().body(response);
            }

            PaymentResponseDto payment = paymentService.updatePaymentStatus(id, status);

            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công");
            response.put("data", payment);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating payment status: ", e);
            response.put("success", false);
            response.put("message", "Lỗi cập nhật trạng thái: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy thống kê payments
     */
    @GetMapping("/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentStatistics(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            Map<String, Object> statistics = paymentService.getPaymentStatistics(ownerId);
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("Error getting payment statistics: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy danh sách contracts có thể tạo payment
     */
    @GetMapping("/available-contracts")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAvailableContracts(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            List<Map<String, Object>> contracts = paymentService.getAvailableContractsForPayment(ownerId);
            // Thêm log để debug
            log.info("Available contracts for owner {}: {}", ownerId, contracts);
            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            log.error("Error getting available contracts: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tính toán chi phí utilities với đơn giá từ frontend
     */
    @PostMapping("/calculate-utility")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calculateUtilityCosts(@RequestBody Map<String, Object> request) {
        try {
            Integer previousReading = (Integer) request.get("previousReading");
            Integer currentReading = (Integer) request.get("currentReading");
            String utilityType = (String) request.get("utilityType");
            Float unitPrice = request.get("unitPrice") != null ? Float.valueOf(request.get("unitPrice").toString())
                    : 0f;

            Map<String, Object> result = paymentService.calculateUtilityCosts(previousReading, currentReading,
                    utilityType, unitPrice);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error calculating utility costs: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tạo payment/invoice mới từ form - cho phép chủ trọ nhập đơn giá điện nước
     */
    @PostMapping("/create-form")
    public String createPaymentFromForm(
            @RequestParam("contractId") Integer contractId,
            @RequestParam("month") String month,
            @RequestParam("roomFee") Float roomFee,
            @RequestParam("wifiFee") Float wifiFee,
            @RequestParam("electricityPrev") Integer electricityPrev,
            @RequestParam("electricityCurr") Integer electricityCurr,
            @RequestParam("electricityUnitPrice") Float electricityUnitPrice,
            @RequestParam("waterPrev") Integer waterPrev,
            @RequestParam("waterCurr") Integer waterCurr,
            @RequestParam("waterUnitPrice") Float waterUnitPrice,
            @RequestParam("trashFee") Float trashFee,
            @RequestParam(value = "otherFee", defaultValue = "0") Float otherFee,
            @RequestParam(value = "notes", defaultValue = "") String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            // Tính toán các khoản phí với đơn giá do chủ trọ nhập
            Integer electricityUsage = electricityCurr - electricityPrev;
            Float electricityFee = electricityUsage * electricityUnitPrice;

            Integer waterUsage = waterCurr - waterPrev;
            Float waterFee = waterUsage * waterUnitPrice;

            // Tạo due date (ngày 10 của tháng tiếp theo)
            String[] monthYear = month.split("-");
            int year = Integer.parseInt(monthYear[0]);
            int monthNum = Integer.parseInt(monthYear[1]);

            LocalDate dueDate = LocalDate.of(year, monthNum, 10);
            String formattedMonth = String.format("%02d/%d", monthNum, year);

            // Tạo PaymentRequestDto
            List<PaymentRequestDto.PaymentDetailDto> details = new ArrayList<>();

            details.add(PaymentRequestDto.PaymentDetailDto.builder()
                    .itemName("Tiền phòng")
                    .quantity(1)
                    .unitPrice(roomFee)
                    .amount(roomFee)
                    .build());

            details.add(PaymentRequestDto.PaymentDetailDto.builder()
                    .itemName("Tiền điện")
                    .quantity(electricityUsage)
                    .unitPrice(electricityUnitPrice)
                    .amount(electricityFee)
                    .previousReading(electricityPrev)
                    .currentReading(electricityCurr)
                    .build());

            details.add(PaymentRequestDto.PaymentDetailDto.builder()
                    .itemName("Tiền nước")
                    .quantity(waterUsage)
                    .unitPrice(waterUnitPrice)
                    .amount(waterFee)
                    .previousReading(waterPrev)
                    .currentReading(waterCurr)
                    .build());

            details.add(PaymentRequestDto.PaymentDetailDto.builder()
                    .itemName("Tiền rác")
                    .quantity(1)
                    .unitPrice(trashFee)
                    .amount(trashFee)
                    .build());

            details.add(PaymentRequestDto.PaymentDetailDto.builder()
                    .itemName("Tiền wifi")
                    .quantity(1)
                    .unitPrice(wifiFee)
                    .amount(wifiFee)
                    .build());

            // Thêm phí khác nếu có
            if (otherFee > 0) {
                details.add(PaymentRequestDto.PaymentDetailDto.builder()
                        .itemName("Phí khác")
                        .quantity(1)
                        .unitPrice(otherFee)
                        .amount(otherFee)
                        .build());
            }

            PaymentRequestDto paymentRequest = PaymentRequestDto.builder()
                    .contractId(contractId)
                    .month(formattedMonth)
                    .dueDate(Date.valueOf(dueDate))
                    .notes(notes)
                    .details(details)
                    .build();

            // Tạo payment
            PaymentResponseDto createdPayment = paymentService.createPayment(paymentRequest);

            log.info("Successfully created payment with ID: {} for contract: {}",
                    createdPayment.getPaymentId(), contractId);

            redirectAttributes.addFlashAttribute("successMessage", "Tạo hóa đơn thành công!");

        } catch (Exception e) {
            log.error("Error creating payment for contract {}: ", contractId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo hóa đơn: " + e.getMessage());
        }

        return "redirect:/chu-tro/thanh-toan";
    }

    /**
     * Gửi tất cả hóa đơn chưa thanh toán hoặc quá hạn đến người thuê
     */
    @PostMapping("/send-unpaid")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendUnpaidInvoices(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            // Lấy danh sách hóa đơn chưa thanh toán hoặc quá hạn
            List<PaymentResponseDto> unpaidPayments = paymentService.getPaymentsByOwnerIdAndStatus(ownerId, PaymentStatus.CHƯA_THANH_TOÁN);
            List<PaymentResponseDto> overduePayments = paymentService.getPaymentsByOwnerIdAndStatus(ownerId, PaymentStatus.QUÁ_HẠN_THANH_TOÁN);

            List<PaymentResponseDto> paymentsToSend = new ArrayList<>();
            paymentsToSend.addAll(unpaidPayments);
            paymentsToSend.addAll(overduePayments);

            if (paymentsToSend.isEmpty()) {
                response.put("success", true);
                response.put("message", "Không có hóa đơn chưa thanh toán hoặc quá hạn để gửi");
                response.put("sentCount", 0);
                return ResponseEntity.ok(response);
            }

            // Gửi từng hóa đơn đến người thuê
            int sentCount = paymentService.sendInvoicesToTenants(paymentsToSend);

            log.info("Successfully sent {} unpaid/overdue invoices for owner {}", sentCount, ownerId);

            response.put("success", true);
            response.put("message", "Gửi " + sentCount + " hóa đơn thành công");
            response.put("sentCount", sentCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending unpaid invoices: ", e);
            response.put("success", false);
            response.put("message", "Lỗi gửi hóa đơn: " + e.getMessage());
            response.put("sentCount", 0);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePayment(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            paymentService.deletePayment(id);
            response.put("success", true);
            response.put("message", "Xóa hóa đơn thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting payment with id {}: ", id, e);
            response.put("success", false);
            response.put("message", "Lỗi xóa hóa đơn: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}