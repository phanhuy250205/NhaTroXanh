package nhatroxanh.com.Nhatroxanh.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.PaymentsRepository;
import nhatroxanh.com.Nhatroxanh.Service.WalletService;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoService {

    private final PaymentsRepository paymentsRepository;
    private final NotificationService notificationService;
    private final WalletService walletService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @Value("${momo.partnerCode:MOMO}")
    private String momoPartnerCode;

    @Value("${momo.accessKey:F8BBA842ECF85}")
    private String momoAccessKey;

    @Value("${momo.secretKey:K951B6PE1waDMi640xX08PD3vg6EkVlz}")
    private String momoSecretKey;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;

    @Value("${momo.callbackUrl:http://localhost:8082/momo/callback}")
    private String momoCallbackUrl;

    @Value("${momo.returnUrl:http://localhost:8082/momo/return}")
    private String momoReturnUrl;

    @Value("${momo.queryUrl:https://test-payment.momo.vn/v2/gateway/api/query}")
    private String momoQueryUrl;

    public static class PaymentResponse {
        private String payUrl;
        private String qrCodeUrl;
        private int resultCode;
        private String message;

        public PaymentResponse(String payUrl, String qrCodeUrl, int resultCode, String message) {
            this.payUrl = payUrl;
            this.qrCodeUrl = qrCodeUrl;
            this.resultCode = resultCode;
            this.message = message;
        }

        public String getPayUrl() { return payUrl; }
        public String getQrCodeUrl() { return qrCodeUrl; }
        public int getResultCode() { return resultCode; }
        public String getMessage() { return message; }
    }

    /**
     * Create MoMo payment order
     */
    public PaymentResponse createMoMoOrder(String invoiceId) {
        try {
            log.info("Creating MoMo order for invoiceId={} at {}", invoiceId, LocalDateTime.now());

            Integer paymentId = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + invoiceId));

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                throw new IllegalStateException("Hóa đơn đã được thanh toán.");
            }

            String requestId = momoPartnerCode + System.currentTimeMillis();
            String orderId = requestId;
            String orderInfo = "Thanh toán hóa đơn #" + invoiceId;
            long amount = Math.round(payment.getTotalAmount());
            String extraData = "invoiceId=" + invoiceId;

            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=payWithATM",
                    momoAccessKey, amount, extraData, momoCallbackUrl, orderId, orderInfo, momoPartnerCode, momoReturnUrl, requestId);
            String signature = generateHmacSHA256(rawSignature);

            JSONObject requestBody = new JSONObject();
            requestBody.put("partnerCode", momoPartnerCode);
            requestBody.put("accessKey", momoAccessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", momoReturnUrl);
            requestBody.put("ipnUrl", momoCallbackUrl);
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", "payWithATM");
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(momoEndpoint);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONObject jsonResponse = new JSONObject(result.toString());
                int resultCode = jsonResponse.getInt("resultCode");
                String message = jsonResponse.optString("message", "No message");

                if (resultCode == 0) {
                    String payUrl = jsonResponse.getString("payUrl");
                    String qrCodeUrl = jsonResponse.optString("qrCodeUrl", null);
                    if (qrCodeUrl == null || qrCodeUrl.isEmpty()) {
                        qrCodeUrl = "https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=" + payUrl;
                    }
                    payment.setAppTransId(orderId);
                    paymentsRepository.save(payment);
                    log.info("MoMo order created successfully: payUrl={}, orderId={}", payUrl, orderId);
                    return new PaymentResponse(payUrl, qrCodeUrl, resultCode, message);
                } else {
                    log.warn("MoMo order creation failed: resultCode={}, message={}", resultCode, message);
                    return new PaymentResponse(null, null, resultCode, "Lỗi MoMo: " + message);
                }
            }
        } catch (Exception e) {
            log.error("Error creating MoMo order for invoiceId {}: {}", invoiceId, e.getMessage(), e);
            return new PaymentResponse(null, null, -1, "Failed to create payment request: " + e.getMessage());
        }
    }

    /**
     * Handle MoMo IPN callback
     */
    public ResponseEntity<Map<String, Object>> handleMoMoCallback(Map<String, Object> callbackData) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Received MoMo callback: {}", callbackData);

            String partnerCode = (String) callbackData.get("partnerCode");
            String orderId = (String) callbackData.get("orderId");
            String requestId = (String) callbackData.get("requestId");
            Long amount = ((Number) callbackData.get("amount")).longValue();
            String orderInfo = (String) callbackData.get("orderInfo");
            String orderType = (String) callbackData.get("orderType");
            String transId = String.valueOf(callbackData.get("transId"));
            Integer resultCode = (Integer) callbackData.get("resultCode");
            String message = (String) callbackData.get("message");
            String payType = (String) callbackData.get("payType");
            String responseTime = String.valueOf(callbackData.get("responseTime"));
            String extraData = (String) callbackData.get("extraData");
            String signature = (String) callbackData.get("signature");

            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                    momoAccessKey, amount, extraData, message, orderId, orderInfo, orderType, partnerCode, payType, requestId, responseTime, resultCode, transId);
            String computedSignature = generateHmacSHA256(rawSignature);

            if (!computedSignature.equals(signature)) {
                log.error("Invalid signature. Expected: {}, Received: {}", computedSignature, signature);
                response.put("resultCode", -1);
                response.put("message", "Invalid signature");
                return ResponseEntity.badRequest().body(response);
            }

            String invoiceId = null;
            if (extraData != null && extraData.contains("invoiceId=")) {
                invoiceId = extraData.split("invoiceId=")[1].split("&")[0];
            }

            if (invoiceId == null) {
                log.error("Invoice ID not found in extraData: {}", extraData);
                response.put("resultCode", -1);
                response.put("message", "Invoice ID not found");
                return ResponseEntity.badRequest().body(response);
            }

            Payments payment = paymentsRepository.findByAppTransId(orderId)
                    .orElse(paymentsRepository.findById(Integer.parseInt(invoiceId)).orElse(null));

            if (payment == null) {
                log.error("Payment not found for orderId: {} or invoiceId: {}", orderId, invoiceId);
                response.put("resultCode", -1);
                response.put("message", "Payment not found");
                return ResponseEntity.badRequest().body(response);
            }

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                log.info("Payment already processed: {}", payment.getId());
                response.put("resultCode", 0);
                response.put("message", "Payment already processed");
                return ResponseEntity.ok(response);
            }

            if (resultCode == 0) {
                payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
                payment.setPaymentMethod(Payments.PaymentMethod.MOMO);
                payment.setPaymentDate(new java.sql.Date(System.currentTimeMillis()));
                paymentsRepository.save(payment);

                try {
                    Contracts contract = payment.getContract();
                    if (contract != null && contract.getTenant() != null) {
                        Users tenant = contract.getTenant();
                        String roomName = contract.getRoom() != null ? contract.getRoom().getNamerooms() : "N/A";
                        String hostelName = contract.getRoom() != null && contract.getRoom().getHostel() != null
                                ? contract.getRoom().getHostel().getName() : "N/A";
                        String formattedAmount = CURRENCY_FORMAT.format(payment.getTotalAmount()) + " VNĐ";

                        String title = "Thanh toán thành công";
                        String notificationMessage = String.format(
                                "Bạn đã thanh toán thành công hóa đơn #%d cho phòng %s tại %s. Số tiền: %s. Phương thức: MoMo. Cảm ơn bạn đã thanh toán đúng hạn!",
                                payment.getId(), roomName, hostelName, formattedAmount);

                        notificationService.handlePaymentSuccess(tenant, payment, title, notificationMessage);
                        log.info("Payment success notification handled for payment: {}", payment.getId());
                        
                        // Add payment amount to landlord's balance
                        try {
                            Users updatedLandlord = walletService.addPaymentToLandlordBalance(payment);
                            log.info("Successfully added payment amount {} to landlord {} balance for MoMo payment {}", 
                                    formattedAmount, updatedLandlord.getUserId(), payment.getId());
                        } catch (Exception walletException) {
                            log.error("Failed to add payment to landlord balance for MoMo payment {}: {}", 
                                    payment.getId(), walletException.getMessage(), walletException);
                            // Don't fail the entire payment process if wallet update fails
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to send notification for payment {}: {}", payment.getId(), e.getMessage());
                }

                log.info("MoMo payment processed successfully: paymentId={}, orderId={}", payment.getId(), orderId);
                response.put("resultCode", 0);
                response.put("message", "Payment processed successfully");
            } else {
                log.warn("MoMo payment failed for orderId: {}, resultCode: {}, message: {}", orderId, resultCode, message);
                response.put("resultCode", resultCode);
                response.put("message", message);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing MoMo callback: {}", e.getMessage(), e);
            response.put("resultCode", -1);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Handle MoMo return URL
     * MoMo typically sends minimal parameters in return URL, so we need to query payment status
     */
    public String handlePaymentReturn(String invoiceId, String orderId, String resultCode, String message,
                                     Integer roomId, Integer hostelId, Integer addressId, Model model) {
        try {
            log.info("MoMo return with invoiceId={}, orderId={}, resultCode={}, message={}", 
                    invoiceId, orderId, resultCode, message);

            Payments payment = null;

            // First, try to find payment by orderId (most reliable)
            if (orderId != null && !orderId.trim().isEmpty()) {
                payment = paymentsRepository.findByAppTransId(orderId).orElse(null);
                log.info("Found payment by orderId: {}", payment != null ? payment.getId() : "null");
            }

            // Fallback: try to find by invoiceId
            if (payment == null && invoiceId != null && !invoiceId.trim().isEmpty()) {
                try {
                    payment = paymentsRepository.findById(Integer.parseInt(invoiceId)).orElse(null);
                    log.info("Found payment by invoiceId: {}", payment != null ? payment.getId() : "null");
                } catch (NumberFormatException e) {
                    log.warn("Invalid invoiceId format: {}", invoiceId);
                }
            }

            if (payment == null) {
                log.warn("Payment not found for invoiceId={}, orderId={}", invoiceId, orderId);
                StringBuilder redirectUrl = new StringBuilder("redirect:/guest/failure-thanhtoan");
                redirectUrl.append("?errorMessage=").append(URLEncoder.encode("Không tìm thấy thông tin thanh toán", StandardCharsets.UTF_8));
                appendRedirectParams(redirectUrl, invoiceId, roomId, hostelId, addressId);
                return redirectUrl.toString();
            }

            // If payment is already completed, redirect to success immediately
            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                log.info("Payment already completed, redirecting to success page for payment ID: {}", payment.getId());
                StringBuilder redirectUrl = new StringBuilder("redirect:/guest/success-thanhtoan?invoiceId=" + payment.getId());
                appendRedirectParams(redirectUrl, null, roomId, hostelId, addressId);
                return redirectUrl.toString();
            }

            // For pending payments, query MoMo for the latest status
            // This is crucial because MoMo return URL doesn't always contain the final status
            if (orderId != null && !orderId.trim().isEmpty()) {
                log.info("Payment pending, querying MoMo status for orderId: {}", orderId);
                
                // Enhanced retry logic with exponential backoff
                int maxRetries = 5;
                int baseDelayMs = 500;
                boolean paymentUpdated = false;
                
                for (int i = 0; i < maxRetries; i++) {
                    try {
                        log.info("Query attempt {} for orderId: {}", i + 1, orderId);
                        ResponseEntity<Map<String, Object>> queryResponse = queryPaymentStatus(orderId);
                        
                        if (queryResponse.getStatusCode() == HttpStatus.OK && 
                            queryResponse.getBody() != null && 
                            Boolean.TRUE.equals(queryResponse.getBody().get("success"))) {
                            
                            Map<String, Object> momoResult = (Map<String, Object>) queryResponse.getBody().get("data");
                            if (momoResult != null) {
                                Integer queryResultCode = (Integer) momoResult.get("resultCode");
                                String queryMessage = (String) momoResult.get("message");
                                
                                log.info("MoMo query result: resultCode={}, message={}", queryResultCode, queryMessage);
                                
                                if (queryResultCode != null && queryResultCode == 0) {
                                    // Payment successful
                                    payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
                                    payment.setPaymentMethod(Payments.PaymentMethod.MOMO);
                                    payment.setPaymentDate(new java.sql.Date(System.currentTimeMillis()));
                                    paymentsRepository.save(payment);
                                    
                                    // Send success notification and add money to landlord's balance
                                    try {
                                        Contracts contract = payment.getContract();
                                        if (contract != null && contract.getTenant() != null) {
                                            Users tenant = contract.getTenant();
                                            String roomName = contract.getRoom() != null ? contract.getRoom().getNamerooms() : "N/A";
                                            String hostelName = contract.getRoom() != null && contract.getRoom().getHostel() != null
                                                    ? contract.getRoom().getHostel().getName() : "N/A";
                                            String formattedAmount = CURRENCY_FORMAT.format(payment.getTotalAmount()) + " VNĐ";

                                            String title = "Thanh toán thành công";
                                            String notificationMessage = String.format(
                                                    "Bạn đã thanh toán thành công hóa đơn #%d cho phòng %s tại %s. Số tiền: %s. Phương thức: MoMo. Cảm ơn bạn đã thanh toán đúng hạn!",
                                                    payment.getId(), roomName, hostelName, formattedAmount);

                                            notificationService.handlePaymentSuccess(tenant, payment, title, notificationMessage);
                                            log.info("Payment success notification sent for payment: {}", payment.getId());
                                            
                                            // Add payment amount to landlord's balance
                                            try {
                                                Users updatedLandlord = walletService.addPaymentToLandlordBalance(payment);
                                                log.info("Successfully added payment amount {} to landlord {} balance for MoMo payment {} (return URL)", 
                                                        formattedAmount, updatedLandlord.getUserId(), payment.getId());
                                            } catch (Exception walletException) {
                                                log.error("Failed to add payment to landlord balance for MoMo payment {} (return URL): {}", 
                                                        payment.getId(), walletException.getMessage(), walletException);
                                                // Don't fail the entire payment process if wallet update fails
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.error("Failed to send notification for payment {}: {}", payment.getId(), e.getMessage());
                                    }
                                    
                                    log.info("Payment status updated to ĐÃ_THANH_TOÁN after query for orderId: {}", orderId);
                                    paymentUpdated = true;
                                    break;
                                } else if (queryResultCode != null && queryResultCode != 1000) {
                                    // Payment failed (not pending)
                                    log.warn("Payment failed with resultCode: {} for orderId: {}", queryResultCode, orderId);
                                    break;
                                }
                                // If resultCode is 1000 (pending), continue retrying
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error querying MoMo status for orderId {} (attempt {}): {}", orderId, i + 1, e.getMessage());
                    }
                    
                    // Wait before next retry with exponential backoff
                    if (i < maxRetries - 1) {
                        int delayMs = baseDelayMs * (int) Math.pow(2, i);
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                if (paymentUpdated) {
                    // Refresh payment status from database
                    payment = paymentsRepository.findById(payment.getId()).orElse(payment);
                }
            }

            log.info("Final payment status for ID {}: {}", payment.getId(), payment.getPaymentStatus());

            // Final decision based on payment status
            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                log.info("Payment successful, redirecting to success page for payment ID: {}", payment.getId());
                StringBuilder redirectUrl = new StringBuilder("redirect:/guest/success-thanhtoan?invoiceId=" + payment.getId());
                appendRedirectParams(redirectUrl, null, roomId, hostelId, addressId);
                return redirectUrl.toString();
            } else {
                log.info("Payment not completed for invoiceId={}, status={}", payment.getId(), payment.getPaymentStatus());
                StringBuilder redirectUrl = new StringBuilder("redirect:/guest/failure-thanhtoan?invoiceId=" + payment.getId());

                String errorMessage;
                if (payment.getPaymentStatus() == Payments.PaymentStatus.CHƯA_THANH_TOÁN) {
                    errorMessage = "Thanh toán chưa được thực hiện hoặc đang được xử lý. Vui lòng kiểm tra lại sau vài phút.";
                } else if (payment.getPaymentStatus() == Payments.PaymentStatus.QUÁ_HẠN_THANH_TOÁN) {
                    errorMessage = "Thanh toán đã quá hạn";
                } else {
                    errorMessage = message != null ? message : "Thanh toán chưa hoàn tất";
                }

                redirectUrl.append("&errorMessage=").append(URLEncoder.encode(errorMessage, StandardCharsets.UTF_8));
                appendRedirectParams(redirectUrl, null, roomId, hostelId, addressId);
                return redirectUrl.toString();
            }

        } catch (Exception e) {
            log.error("Error processing MoMo return: {}", e.getMessage(), e);
            StringBuilder redirectUrl = new StringBuilder("redirect:/guest/failure-thanhtoan");
            redirectUrl.append("?errorMessage=").append(URLEncoder.encode("Lỗi xử lý thanh toán: " + e.getMessage(), StandardCharsets.UTF_8));
            appendRedirectParams(redirectUrl, invoiceId, roomId, hostelId, addressId);
            return redirectUrl.toString();
        }
    }

    /**
     * Query payment status from MoMo
     */
    public ResponseEntity<Map<String, Object>> queryPaymentStatus(String orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String requestId = "QUERY_" + System.currentTimeMillis();

            String rawSignature = String.format(
                    "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
                    momoAccessKey, orderId, momoPartnerCode, requestId);
            String signature = generateHmacSHA256(rawSignature);

            JSONObject requestBody = new JSONObject();
            requestBody.put("partnerCode", momoPartnerCode);
            requestBody.put("requestId", requestId);
            requestBody.put("orderId", orderId);
            requestBody.put("lang", "vi");
            requestBody.put("signature", signature);

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(momoQueryUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONObject jsonResponse = new JSONObject(result.toString());
                response.put("success", true);
                response.put("data", objectMapper.readValue(jsonResponse.toString(), Map.class));
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error querying MoMo payment status for orderId {}: {}", orderId, e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Lỗi truy vấn trạng thái thanh toán: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to append redirect parameters
     */
    private void appendRedirectParams(StringBuilder redirectUrl, String invoiceId, Integer roomId, Integer hostelId, Integer addressId) {
        if (invoiceId != null) redirectUrl.append("&invoiceId=").append(invoiceId);
        if (roomId != null) redirectUrl.append("&room_id=").append(roomId);
        if (hostelId != null) redirectUrl.append("&hostel_id=").append(hostelId);
        if (addressId != null) redirectUrl.append("&address_id=").append(addressId);
    }

    /**
     * Generate HMAC SHA256 signature
     */
    private String generateHmacSHA256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(momoSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC SHA256: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating signature", e);
        }
    }
}