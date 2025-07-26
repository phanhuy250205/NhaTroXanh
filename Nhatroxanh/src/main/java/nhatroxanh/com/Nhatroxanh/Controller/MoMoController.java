package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Service.MoMoService;
import nhatroxanh.com.Nhatroxanh.Service.MoMoService.PaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MoMoController {

    private final MoMoService momoService;

    /**
     * Create MoMo payment order
     */
    @PostMapping("/momo/create-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createMoMoOrder(@RequestParam("invoiceId") String invoiceId) {
        log.info("Received request to create MoMo order for invoiceId={}", invoiceId);
        PaymentResponse response = momoService.createMoMoOrder(invoiceId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", response.getResultCode() == 0);
        result.put("payUrl", response.getPayUrl());
        result.put("qrCodeUrl", response.getQrCodeUrl());
        result.put("message", response.getMessage());
        
        return response.getResultCode() == 0 
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Handle MoMo IPN callback
     */
    @PostMapping("/momo/callback")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleMoMoCallback(@RequestBody Map<String, Object> callbackData) {
        log.info("Received MoMo callback at {}", java.time.LocalDateTime.now());
        return momoService.handleMoMoCallback(callbackData);
    }

    /**
     * Handle MoMo return URL
     */
    @GetMapping("/momo/return")
    public String momoReturn(
            @RequestParam(value = "invoiceId", required = false) String invoiceId,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "resultCode", required = false) String resultCode,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            Model model) {
        log.info("MoMo return endpoint called with invoiceId={}, orderId={}, resultCode={}, message={}", 
                invoiceId, orderId, resultCode, message);
        return momoService.handlePaymentReturn(invoiceId, orderId, resultCode, message, roomId, hostelId, addressId, model);
    }

    /**
     * Generic payment return endpoint for MoMo
     */
    @GetMapping("/payment/momo/return")
    public String paymentMoMoReturn(
            @RequestParam(value = "invoiceId", required = false) String invoiceId,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "resultCode", required = false) String resultCode,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            Model model) {
        log.info("Payment MoMo return endpoint called with invoiceId={}, orderId={}, resultCode={}, message={}", 
                invoiceId, orderId, resultCode, message);
        return momoService.handlePaymentReturn(invoiceId, orderId, resultCode, message, roomId, hostelId, addressId, model);
    }
}