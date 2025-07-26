package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Service.ZaloPayService;
import nhatroxanh.com.Nhatroxanh.Service.ZaloPayService.PaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ZaloPayController {

    private final ZaloPayService zaloPayService;

    /**
     * Create ZaloPay payment order
     */
    @PostMapping("/zalopay/create-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createZaloPayOrder(@RequestParam(value = "invoiceId", required = false) String invoiceId) {
        log.info("Received request to create ZaloPay order with invoiceId={}", invoiceId);
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "invoiceId is required");
            return ResponseEntity.badRequest().body(result);
        }
        
        PaymentResponse response = zaloPayService.createZaloPayOrder(invoiceId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", response.getResultCode() == 1);
        result.put("payUrl", response.getPayUrl());
        result.put("qrCodeUrl", response.getQrCodeUrl());
        result.put("orderToken", response.getOrderToken());
        result.put("message", response.getMessage());
        
        return response.getResultCode() == 1 
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Handle ZaloPay callback
     */
    @PostMapping("/zalopay/callback")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleZaloPayCallback(@RequestBody Map<String, Object> callbackData) {
        log.info("Received ZaloPay callback at {}", java.time.LocalDateTime.now());
        return zaloPayService.handleZaloPayCallback(callbackData);
    }

    /**
     * Handle ZaloPay return URL
     */
    @GetMapping("/zalopay/return")
    public String zaloPayReturn(
            @RequestParam(value = "invoiceId", required = false) String invoiceId,
            @RequestParam(value = "appTransId", required = false) String appTransId,
            @RequestParam(value = "apptransid", required = false) String apptransid,
            @RequestParam(value = "resultCode", required = false) String resultCode,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            Model model) {
        // Map ZaloPay's parameters
        String effectiveAppTransId = apptransid != null ? apptransid : appTransId;
        String effectiveResultCode = status != null ? status : resultCode;
        
        log.debug("Raw ZaloPay return parameters: apptransid={}, status={}, invoiceId={}, appTransId={}, resultCode={}, message={}",
                apptransid, status, invoiceId, appTransId, resultCode, message);
        log.info("ZaloPay return endpoint called with invoiceId={}, appTransId={}, resultCode={}, message={}",
                invoiceId, effectiveAppTransId, effectiveResultCode, message);
        return zaloPayService.handlePaymentReturn(invoiceId, effectiveAppTransId, effectiveResultCode, message, roomId, hostelId, addressId, model);
    }

    /**
     * Generic payment return endpoint for ZaloPay
     */
    @GetMapping("/payment/zalopay/return")
    public String paymentZaloPayReturn(
            @RequestParam(value = "invoiceId", required = false) String invoiceId,
            @RequestParam(value = "appTransId", required = false) String appTransId,
            @RequestParam(value = "apptransid", required = false) String apptransid,
            @RequestParam(value = "resultCode", required = false) String resultCode,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            Model model) {
        // Map ZaloPay's parameters
        String effectiveAppTransId = apptransid != null ? apptransid : appTransId;
        String effectiveResultCode = status != null ? status : resultCode;
        
        log.debug("Raw ZaloPay payment return parameters: apptransid={}, status={}, invoiceId={}, appTransId={}, resultCode={}, message={}",
                apptransid, status, invoiceId, appTransId, resultCode, message);
        log.info("Payment ZaloPay return endpoint called with invoiceId={}, appTransId={}, resultCode={}, message={}",
                invoiceId, effectiveAppTransId, effectiveResultCode, message);
        return zaloPayService.handlePaymentReturn(invoiceId, effectiveAppTransId, effectiveResultCode, message, roomId, hostelId, addressId, model);
    }

    /**
     * Query ZaloPay payment status
     */
    @PostMapping("/zalopay/query-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> queryPaymentStatus(@RequestParam("appTransId") String appTransId) {
        log.info("Received request to query ZaloPay status for appTransId={}", appTransId);
        return zaloPayService.queryPaymentStatus(appTransId);
    }

    /**
     * Get QR code for ZaloPay payment
     */
    @PostMapping("/zalopay/get-qr")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getZaloPayQR(@RequestParam(value = "invoiceId", required = false) String invoiceId) {
        log.info("Received request to get ZaloPay QR for invoiceId={}", invoiceId);
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "invoiceId is required");
            return ResponseEntity.badRequest().body(result);
        }
        
        PaymentResponse response = zaloPayService.createZaloPayOrder(invoiceId);
        Map<String, Object> result = new HashMap<>();
        if (response.getResultCode() == 1) {
            result.put("success", true);
            result.put("qrCodeUrl", response.getQrCodeUrl());
            result.put("payUrl", response.getPayUrl());
            result.put("orderToken", response.getOrderToken());
            result.put("message", "QR code generated successfully");
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", response.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}