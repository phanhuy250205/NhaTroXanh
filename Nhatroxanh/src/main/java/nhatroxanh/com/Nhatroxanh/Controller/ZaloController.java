package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.PaymentsRepository;
import nhatroxanh.com.Nhatroxanh.Service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ZaloController {

    private final PaymentsRepository paymentsRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Value("${zalopay.appId}")
    private String zaloPayAppId;

    @Value("${zalopay.key1}")
    private String zaloPayKey1;

    @Value("${zalopay.key2}")
    private String zaloPayKey2;

    @Value("${zalopay.endpoint}")
    private String zaloPayEndpoint;

    @Value("${zalopay.callbackUrl}")
    private String zaloPayCallbackUrl;

    /**
     * Creates a ZaloPay order for the given payment.
     */
    @PostMapping("/zalopay/create-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createZaloPayOrder(
            @RequestParam("invoiceId") String invoiceId) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Creating ZaloPay order for invoiceId={} at {}", invoiceId, LocalDateTime.now());

            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt)
                    .orElseThrow(() -> {
                        log.error("Payment not found with id: {} at {}", invoiceId, LocalDateTime.now());
                        return new IllegalArgumentException("Payment not found with id: " + invoiceId);
                    });

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                throw new IllegalStateException("Hóa đơn đã được thanh toán.");
            }

            // Prepare ZaloPay order data
            String appTransId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "_"
                    + System.currentTimeMillis();
            long appTime = System.currentTimeMillis();
            long amount = Math.round(payment.getTotalAmount());
            String embedData = "{\"invoiceId\":\"" + invoiceId + "\"}";
            String item = "[]";
            String description = "Thanh toán hóa đơn #" + invoiceId;

            // Generate MAC for request
            String macData = zaloPayAppId + "|" + appTransId + "|" + "user123" + "|" +
                    amount + "|" + appTime + "|" + embedData + "|" + item;
            String mac = generateMac(macData, zaloPayKey1);

            // Prepare form data for ZaloPay API
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("app_id", zaloPayAppId);
            formData.add("app_trans_id", appTransId);
            formData.add("app_time", String.valueOf(appTime));
            formData.add("app_user", "user123");
            formData.add("amount", String.valueOf(amount));
            formData.add("description", description);
            formData.add("embed_data", embedData);
            formData.add("item", item);
            formData.add("bank_code", "");
            formData.add("callback_url", zaloPayCallbackUrl);
            formData.add("mac", mac);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

            log.info("Sending ZaloPay request with data: appId={}, appTransId={}, amount={}", 
                    zaloPayAppId, appTransId, amount);

            // Send request to ZaloPay Sandbox
            ResponseEntity<String> zaloPayResponse = restTemplate.postForEntity(zaloPayEndpoint, requestEntity, String.class);
            
            log.info("ZaloPay response status: {}, body: {}", zaloPayResponse.getStatusCode(), zaloPayResponse.getBody());
            
            Map<String, Object> zaloPayResult = objectMapper.readValue(zaloPayResponse.getBody(), Map.class);

            if (zaloPayResult.get("return_code") != null && (Integer) zaloPayResult.get("return_code") == 1) {
                // Save appTransId to payment for callback verification
                payment.setAppTransId(appTransId);
                paymentsRepository.save(payment);

                response.put("success", true);
                response.put("orderUrl", zaloPayResult.get("order_url"));
                response.put("appTransId", appTransId);
                response.put("message", "ZaloPay order created successfully!");
                
                log.info("ZaloPay order created successfully for invoiceId: {}, appTransId: {}", invoiceId, appTransId);
                return ResponseEntity.ok(response);
            } else {
                String errorMessage = (String) zaloPayResult.get("return_message");
                log.error("ZaloPay order creation failed for invoiceId: {} at {}. Error: {}, Full response: {}",
                        invoiceId, LocalDateTime.now(), errorMessage, zaloPayResult);
                response.put("success", false);
                response.put("error", errorMessage != null ? errorMessage : "Tạo đơn hàng ZaloPay thất bại");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {} at {}", invoiceId, LocalDateTime.now(), e);
            response.put("success", false);
            response.put("error", "Mã hóa đơn không hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error creating ZaloPay order for invoiceId: {} at {}. Error: {}",
                    invoiceId, LocalDateTime.now(), e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error creating ZaloPay order for invoiceId: {} at {}. Error: {}",
                    invoiceId, LocalDateTime.now(), e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Lỗi hệ thống khi tạo đơn hàng ZaloPay: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Handles ZaloPay callback for payment status updates.
     */
    @PostMapping("/zalopay/callback")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleZaloPayCallback(@RequestBody String callbackBody) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Received ZaloPay callback at {}: {}", LocalDateTime.now(), callbackBody);

            // Parse callback data
            Map<String, Object> callbackData = objectMapper.readValue(callbackBody, Map.class);
            
            // Verify callback MAC
            String dataStr = (String) callbackData.get("data");
            String reqMac = (String) callbackData.get("mac");
            String computedMac = generateMac(dataStr, zaloPayKey2);

            if (!reqMac.equals(computedMac)) {
                log.error("Invalid MAC in ZaloPay callback at {}. Expected: {}, Received: {}", 
                         LocalDateTime.now(), computedMac, reqMac);
                response.put("return_code", -1);
                response.put("return_message", "Invalid MAC");
                return ResponseEntity.badRequest().body(response);
            }

            // Parse data field
            Map<String, Object> data = objectMapper.readValue(dataStr, Map.class);
            String appTransId = (String) data.get("app_trans_id");
            Number amountNum = (Number) data.get("amount");
            String embedDataStr = (String) data.get("embed_data");
            Map<String, Object> embedData = objectMapper.readValue(embedDataStr, Map.class);
            String invoiceId = (String) embedData.get("invoiceId");

            log.info("Processing callback for appTransId: {}, invoiceId: {}, amount: {}", 
                    appTransId, invoiceId, amountNum);

            // Find payment by appTransId
            Payments payment = paymentsRepository.findByAppTransId(appTransId)
                    .orElseThrow(() -> {
                        log.error("Payment not found for appTransId: {} at {}", appTransId, LocalDateTime.now());
                        return new IllegalArgumentException("Payment not found for appTransId: " + appTransId);
                    });

            if (!invoiceId.equals(String.valueOf(payment.getId()))) {
                log.error("Invoice ID mismatch for appTransId: {} at {}. Expected: {}, Received: {}", 
                         appTransId, LocalDateTime.now(), payment.getId(), invoiceId);
                response.put("return_code", -1);
                response.put("return_message", "Invoice ID mismatch");
                return ResponseEntity.badRequest().body(response);
            }

            // Update payment status - ZaloPay callback means payment was successful
            payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
            payment.setPaymentMethod(Payments.PaymentMethod.ZALOPAY);
            payment.setPaymentDate(new java.sql.Date(System.currentTimeMillis()));
            paymentsRepository.save(payment);
            
            log.info("Payment updated to PAID for invoiceId: {} at {}", invoiceId, LocalDateTime.now());

            // Handle payment success notifications
            try {
                Contracts contract = payment.getContract();
                if (contract != null && contract.getTenant() != null) {
                    Users tenant = contract.getTenant();
                    String roomName = contract.getRoom() != null ? contract.getRoom().getNamerooms() : "N/A";
                    String hostelName = contract.getRoom() != null && contract.getRoom().getHostel() != null 
                        ? contract.getRoom().getHostel().getName() : "N/A";
                    
                    String formattedAmount = CURRENCY_FORMAT.format(payment.getTotalAmount()) + " VNĐ";
                    
                    String title = "Thanh toán thành công";
                    String message = String.format(
                        "Bạn đã thanh toán thành công hóa đơn #%d cho phòng %s tại %s. " +
                        "Số tiền: %s. Phương thức: ZaloPay. " +
                        "Cảm ơn bạn đã thanh toán đúng hạn!",
                        payment.getId(),
                        roomName,
                        hostelName,
                        formattedAmount
                    );

                    // Use the new method to handle payment success
                    notificationService.handlePaymentSuccess(tenant, payment, title, message);
                    log.info("Payment success notifications handled for payment: {}", invoiceId);
                } else {
                    log.warn("Cannot create success notification - contract or tenant not found for payment: {}", invoiceId);
                }
            } catch (Exception e) {
                log.error("Failed to handle payment success notifications for payment: {}, error: {}", invoiceId, e.getMessage());
            }

            response.put("return_code", 1);
            response.put("return_message", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing ZaloPay callback at {}: {}", LocalDateTime.now(), e.getMessage(), e);
            response.put("return_code", -1);
            response.put("return_message", "Error processing callback: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    /**
     * Handles payment success redirect from ZaloPay
     */
    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam(value = "invoiceId", required = false) String invoiceId,
                                @RequestParam(value = "status", required = false) String status,
                                Model model) {
        try {
            log.info("Payment success redirect received for invoiceId: {}, status: {}", invoiceId, status);
            
            if (invoiceId != null) {
                Integer paymentIdInt = Integer.parseInt(invoiceId);
                Payments payment = paymentsRepository.findById(paymentIdInt).orElse(null);
                
                if (payment != null) {
                    model.addAttribute("payment", payment);
                    model.addAttribute("success", true);
                    model.addAttribute("message", "Thanh toán thành công! Hóa đơn #" + invoiceId + " đã được thanh toán.");
                } else {
                    model.addAttribute("success", false);
                    model.addAttribute("message", "Không tìm thấy thông tin hóa đơn.");
                }
            } else {
                model.addAttribute("success", true);
                model.addAttribute("message", "Thanh toán đã được xử lý thành công!");
            }
            
            return "guest/payment-success";
        } catch (Exception e) {
            log.error("Error handling payment success redirect: {}", e.getMessage(), e);
            model.addAttribute("success", false);
            model.addAttribute("message", "Có lỗi xảy ra khi xử lý thanh toán.");
            return "guest/payment-success";
        }
    }

    /**
     * Generates HMAC-SHA256 signature for ZaloPay requests.
     */
    private String generateMac(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
