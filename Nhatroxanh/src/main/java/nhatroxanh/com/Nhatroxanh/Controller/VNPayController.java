package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Service.VNPayService;
import nhatroxanh.com.Nhatroxanh.Service.VNPayService.PaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    private final VNPayService vnPayService;

    /**
     * Create VNPay payment order
     */
    @PostMapping("/vnpay/create-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createVNPayOrder(@RequestParam("invoiceId") String invoiceId) {
        log.info("Received request to create VNPay order for invoiceId={}", invoiceId);
        PaymentResponse response = vnPayService.createVNPayOrder(invoiceId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", response.getResultCode() == 0);
        result.put("paymentUrl", response.getPaymentUrl());
        result.put("message", response.getMessage());
        
        return response.getResultCode() == 0 
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Handle VNPay return URL
     */
    @GetMapping("/vnpay/return")
    public String vnPayReturn(
            @RequestParam Map<String, String> allParams,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            Model model) {
        
        log.info("VNPay return endpoint called with params: {}", allParams);
        return vnPayService.handlePaymentReturn(allParams, roomId, hostelId, addressId, model);
    }

    /**
     * Generic payment return endpoint for VNPay
     */
    @GetMapping("/payment/vnpay/return")
    public String paymentVNPayReturn(
            @RequestParam Map<String, String> allParams,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            Model model) {
        
        log.info("Payment VNPay return endpoint called with params: {}", allParams);
        return vnPayService.handlePaymentReturn(allParams, roomId, hostelId, addressId, model);
    }

    /**
     * Debug endpoint to check VNPay configuration
     */
    @GetMapping("/vnpay/debug-config")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugVNPayConfig() {
        Map<String, Object> debugInfo = new HashMap<>();
        try {
            // Only show non-sensitive configuration info
            debugInfo.put("payUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
            debugInfo.put("returnUrl", "http://localhost:8082/vnpay/return");
            debugInfo.put("version", "2.1.0");
            debugInfo.put("command", "pay");
            debugInfo.put("orderType", "other");
            debugInfo.put("hasTmnCode", System.getProperty("vnpay.tmnCode") != null || 
                                       System.getenv("VNPAY_TMN_CODE") != null);
            debugInfo.put("hasHashSecret", System.getProperty("vnpay.hashSecret") != null || 
                                          System.getenv("VNPAY_HASH_SECRET") != null);
            debugInfo.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            return ResponseEntity.status(500).body(debugInfo);
        }
    }
}
