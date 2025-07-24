package nhatroxanh.com.Nhatroxanh.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.PaymentsRepository;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZaloPayService {

    private final PaymentsRepository paymentsRepository;
    private final NotificationService notificationService;
    private final WalletService walletService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @Value("${zalopay.appId:2553}")
    private String zaloPayAppId;

    @Value("${zalopay.key1:PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL}")
    private String zaloPayKey1;

    @Value("${zalopay.key2:kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz}")
    private String zaloPayKey2;

    @Value("${zalopay.endpoint:https://sb-openapi.zalopay.vn/v2/create}")
    private String zaloPayEndpoint;

    @Value("${zalopay.callbackUrl:http://localhost:8082/zalopay/callback}")
    private String zaloPayCallbackUrl;

    @Value("${zalopay.queryUrl:https://sb-openapi.zalopay.vn/v2/query}")
    private String zaloPayQueryUrl;

    public static class PaymentResponse {
        private String payUrl;
        private String qrCodeUrl;
        private String orderToken;
        private int resultCode;
        private String message;

        public PaymentResponse(String payUrl, String qrCodeUrl, String orderToken, int resultCode, String message) {
            this.payUrl = payUrl;
            this.qrCodeUrl = qrCodeUrl;
            this.orderToken = orderToken;
            this.resultCode = resultCode;
            this.message = message;
        }

        public String getPayUrl() { return payUrl; }
        public String getQrCodeUrl() { return qrCodeUrl; }
        public String getOrderToken() { return orderToken; }
        public int getResultCode() { return resultCode; }
        public String getMessage() { return message; }
    }

    /**
     * Create ZaloPay payment order
     */
    public PaymentResponse createZaloPayOrder(String invoiceId) {
        try {
            log.info("Creating ZaloPay order for invoiceId={} at {}", invoiceId, LocalDateTime.now());

            if (invoiceId == null || invoiceId.trim().isEmpty()) {
                throw new IllegalArgumentException("invoiceId is required");
            }

            Integer paymentId = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + invoiceId));

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                throw new IllegalStateException("Hóa đơn đã được thanh toán.");
            }

            String appTransId = getCurrentDate() + "_" + System.currentTimeMillis();
            String description = "Thanh toán hóa đơn #" + invoiceId;
            long amount = Math.round(payment.getTotalAmount());

            // Create order data
            JSONObject orderData = new JSONObject();
            orderData.put("app_id", Integer.parseInt(zaloPayAppId));
            orderData.put("app_trans_id", appTransId);
            orderData.put("app_user", "user_" + invoiceId);
            orderData.put("app_time", System.currentTimeMillis());
            orderData.put("amount", amount);
            orderData.put("description", description);
            orderData.put("bank_code", "");
            orderData.put("callback_url", zaloPayCallbackUrl);

            // Create embed data
            JSONObject embedData = new JSONObject();
            embedData.put("redirecturl", "http://localhost:8082/zalopay/return");
            embedData.put("invoiceId", invoiceId);
            orderData.put("embed_data", embedData.toString());

            // Create item data
            JSONObject item = new JSONObject();
            item.put("itemid", "invoice_" + invoiceId);
            item.put("itemname", "Hóa đơn #" + invoiceId);
            item.put("itemprice", amount);
            item.put("itemquantity", 1);
            orderData.put("item", "[" + item.toString() + "]");

            // Generate MAC
            String data = orderData.getInt("app_id") + "|" + orderData.getString("app_trans_id") + "|" +
                         orderData.getString("app_user") + "|" + orderData.getLong("amount") + "|" +
                         orderData.getLong("app_time") + "|" + orderData.getString("embed_data") + "|" +
                         orderData.getString("item");
            String mac = generateHmacSHA256(data, zaloPayKey1);
            orderData.put("mac", mac);

            // Send request to ZaloPay
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(zaloPayEndpoint);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            StringBuilder formData = new StringBuilder();
            formData.append("app_id=").append(orderData.getInt("app_id"));
            formData.append("&app_trans_id=").append(orderData.getString("app_trans_id"));
            formData.append("&app_user=").append(orderData.getString("app_user"));
            formData.append("&amount=").append(orderData.getLong("amount"));
            formData.append("&app_time=").append(orderData.getLong("app_time"));
            formData.append("&embed_data=").append(URLEncoder.encode(orderData.getString("embed_data"), StandardCharsets.UTF_8));
            formData.append("&item=").append(URLEncoder.encode(orderData.getString("item"), StandardCharsets.UTF_8));
            formData.append("&description=").append(URLEncoder.encode(orderData.getString("description"), StandardCharsets.UTF_8));
            formData.append("&bank_code=").append(orderData.getString("bank_code"));
            formData.append("&callback_url=").append(URLEncoder.encode(orderData.getString("callback_url"), StandardCharsets.UTF_8));
            formData.append("&mac=").append(orderData.getString("mac"));

            httpPost.setEntity(new StringEntity(formData.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONObject jsonResponse = new JSONObject(result.toString());
                int returnCode = jsonResponse.getInt("return_code");
                String returnMessage = jsonResponse.optString("return_message", "No message");

                if (returnCode == 1) {
                    String orderToken = jsonResponse.getString("order_token");
                    String payUrl = jsonResponse.optString("order_url", "");
                    
                    // Generate QR code URL
                    String qrCodeUrl = "https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=" + 
                                     URLEncoder.encode(payUrl, StandardCharsets.UTF_8);

                    // Save transaction reference to payment
                    payment.setAppTransId(appTransId);
                    paymentsRepository.save(payment);

                    log.info("ZaloPay order created successfully: orderToken={}, appTransId={}", orderToken, appTransId);
                    return new PaymentResponse(payUrl, qrCodeUrl, orderToken, returnCode, returnMessage);
                } else {
                    log.warn("ZaloPay order creation failed: returnCode={}, message={}", returnCode, returnMessage);
                    return new PaymentResponse(null, null, null, returnCode, "Lỗi ZaloPay: " + returnMessage);
                }
            }
        } catch (Exception e) {
            log.error("Error creating ZaloPay order for invoiceId {}: {}", invoiceId, e.getMessage(), e);
            return new PaymentResponse(null, null, null, -1, "Failed to create payment request: " + e.getMessage());
        }
    }

    /**
     * Handle ZaloPay callback
     */
    public ResponseEntity<Map<String, Object>> handleZaloPayCallback(Map<String, Object> callbackData) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Received ZaloPay callback: {}", callbackData);

            String dataStr = (String) callbackData.get("data");
            String reqMac = (String) callbackData.get("mac");

            // Verify MAC
            String computedMac = generateHmacSHA256(dataStr, zaloPayKey2);
            if (!computedMac.equals(reqMac)) {
                log.error("Invalid MAC. Expected: {}, Received: {}", computedMac, reqMac);
                response.put("return_code", -1);
                response.put("return_message", "Invalid MAC");
                return ResponseEntity.badRequest().body(response);
            }

            // Parse data
            JSONObject data = new JSONObject(dataStr);
            String appTransId = data.getString("app_trans_id");
            long amount = data.getLong("amount");
            String embedData = data.getString("embed_data");

            // Extract invoice ID from embed data
            JSONObject embedJson = new JSONObject(embedData);
            String invoiceId = embedJson.getString("invoiceId");

            if (invoiceId == null) {
                log.error("Invoice ID not found in embed data: {}", embedData);
                response.put("return_code", -1);
                response.put("return_message", "Invoice ID not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Find payment
            Payments payment = paymentsRepository.findByAppTransId(appTransId)
                    .orElse(paymentsRepository.findById(Integer.parseInt(invoiceId)).orElse(null));

            if (payment == null) {
                log.error("Payment not found for appTransId: {} or invoiceId: {}", appTransId, invoiceId);
                response.put("return_code", -1);
                response.put("return_message", "Payment not found");
                return ResponseEntity.badRequest().body(response);
            }

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                log.info("Payment already processed: {}", payment.getId());
                response.put("return_code", 1);
                response.put("return_message", "Payment already processed");
                return ResponseEntity.ok(response);
            }

            // Update payment status
            payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
            payment.setPaymentMethod(Payments.PaymentMethod.ZALOPAY);
            payment.setPaymentDate(new java.sql.Date(System.currentTimeMillis()));
            paymentsRepository.save(payment);

            // Send notification and add money to landlord's balance
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
                            "Bạn đã thanh toán thành công hóa đơn #%d cho phòng %s tại %s. Số tiền: %s. Phương thức: ZaloPay. Cảm ơn bạn đã thanh toán đúng hạn!",
                            payment.getId(), roomName, hostelName, formattedAmount);

                    notificationService.handlePaymentSuccess(tenant, payment, title, notificationMessage);
                    log.info("Payment success notification sent for payment: {}", payment.getId());
                    
                    // Add payment amount to landlord's balance
                    try {
                        Users updatedLandlord = walletService.addPaymentToLandlordBalance(payment);
                        log.info("Successfully added payment amount {} to landlord {} balance for ZaloPay payment {}", 
                                formattedAmount, updatedLandlord.getUserId(), payment.getId());
                    } catch (Exception walletException) {
                        log.error("Failed to add payment to landlord balance for ZaloPay payment {}: {}", 
                                payment.getId(), walletException.getMessage(), walletException);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send notification for payment {}: {}", payment.getId(), e.getMessage());
            }

            log.info("ZaloPay payment processed successfully: paymentId={}, appTransId={}", payment.getId(), appTransId);
            response.put("return_code", 1);
            response.put("return_message", "Payment processed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing ZaloPay callback: {}", e.getMessage(), e);
            response.put("return_code", -1);
            response.put("return_message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Query payment status from ZaloPay
     */
    public ResponseEntity<Map<String, Object>> queryPaymentStatus(String appTransId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (appTransId == null || appTransId.trim().isEmpty()) {
                log.error("appTransId is required for querying payment status");
                response.put("success", false);
                response.put("error", "appTransId is required");
                return ResponseEntity.badRequest().body(response);
            }

            String data = zaloPayAppId + "|" + appTransId + "|" + zaloPayKey1;
            String mac = generateHmacSHA256(data, zaloPayKey1);

            JSONObject requestBody = new JSONObject();
            requestBody.put("app_id", Integer.parseInt(zaloPayAppId));
            requestBody.put("app_trans_id", appTransId);
            requestBody.put("mac", mac);

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(zaloPayQueryUrl);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            StringBuilder formData = new StringBuilder();
            formData.append("app_id=").append(requestBody.getInt("app_id"));
            formData.append("&app_trans_id=").append(requestBody.getString("app_trans_id"));
            formData.append("&mac=").append(requestBody.getString("mac"));

            httpPost.setEntity(new StringEntity(formData.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONObject jsonResponse = new JSONObject(result.toString());
                log.debug("ZaloPay query response: {}", jsonResponse.toString());
                response.put("success", true);
                response.put("data", objectMapper.readValue(jsonResponse.toString(), Map.class));
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error querying ZaloPay payment status for appTransId {}: {}", appTransId, e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Lỗi truy vấn trạng thái thanh toán: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Handle ZaloPay return URL
     */
    public String handlePaymentReturn(String invoiceId, String appTransId, String resultCode, String message,
                                     Integer roomId, Integer hostelId, Integer addressId, Model model) {
        try {
            log.info("ZaloPay return with invoiceId={}, appTransId={}, resultCode={}, message={}",
                    invoiceId, appTransId, resultCode, message);

            if (appTransId == null && invoiceId == null) {
                log.warn("Both appTransId and invoiceId are null");
                StringBuilder redirectUrl = new StringBuilder("redirect:/guest/failure-thanhtoan");
                redirectUrl.append("?errorMessage=").append(URLEncoder.encode("Không tìm thấy thông tin thanh toán", StandardCharsets.UTF_8));
                appendRedirectParams(redirectUrl, invoiceId, roomId, hostelId, addressId);
                return redirectUrl.toString();
            }

            Payments payment = null;

            // First, try to find payment by appTransId (most reliable)
            if (appTransId != null && !appTransId.trim().isEmpty()) {
                payment = paymentsRepository.findByAppTransId(appTransId).orElse(null);
                log.info("Found payment by appTransId: {}", payment != null ? payment.getId() : "null");
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
                log.warn("Payment not found for invoiceId={}, appTransId={}", invoiceId, appTransId);
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

            // For pending payments, query ZaloPay for the latest status
            if (appTransId != null && !appTransId.trim().isEmpty()) {
                log.info("Payment pending, querying ZaloPay status for appTransId: {}", appTransId);

                // Enhanced retry logic with exponential backoff
                int maxRetries = 5;
                int baseDelayMs = 500;
                boolean paymentUpdated = false;

                for (int i = 0; i < maxRetries; i++) {
                    try {
                        log.info("Query attempt {} for appTransId: {}", i + 1, appTransId);
                        ResponseEntity<Map<String, Object>> queryResponse = queryPaymentStatus(appTransId);

                        if (queryResponse.getStatusCode() == HttpStatus.OK &&
                                queryResponse.getBody() != null &&
                                Boolean.TRUE.equals(queryResponse.getBody().get("success"))) {

                            Map<String, Object> zaloResult = (Map<String, Object>) queryResponse.getBody().get("data");
                            if (zaloResult != null) {
                                Integer queryResultCode = (Integer) zaloResult.get("return_code");
                                String queryMessage = (String) zaloResult.get("return_message");

                                log.info("ZaloPay query result: return_code={}, message={}", queryResultCode, queryMessage);

                                if (queryResultCode != null && queryResultCode == 1) {
                                    // Payment successful
                                    payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
                                    payment.setPaymentMethod(Payments.PaymentMethod.ZALOPAY);
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
                                                    "Bạn đã thanh toán thành công hóa đơn #%d cho phòng %s tại %s. Số tiền: %s. Phương thức: ZaloPay. Cảm ơn bạn đã thanh toán đúng hạn!",
                                                    payment.getId(), roomName, hostelName, formattedAmount);

                                            notificationService.handlePaymentSuccess(tenant, payment, title, notificationMessage);
                                            log.info("Payment success notification sent for payment: {}", payment.getId());

                                            // Add payment amount to landlord's balance
                                            try {
                                                Users updatedLandlord = walletService.addPaymentToLandlordBalance(payment);
                                                log.info("Successfully added payment amount {} to landlord {} balance for ZaloPay payment {} (return URL)",
                                                        formattedAmount, updatedLandlord.getUserId(), payment.getId());
                                            } catch (Exception walletException) {
                                                log.error("Failed to add payment to landlord balance for ZaloPay payment {} (return URL): {}",
                                                        payment.getId(), walletException.getMessage(), walletException);
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.error("Failed to send notification for payment {}: {}", payment.getId(), e.getMessage());
                                    }

                                    log.info("Payment status updated to ĐÃ_THANH_TOÁN after query for appTransId: {}", appTransId);
                                    paymentUpdated = true;
                                    break;
                                } else if (queryResultCode != null && queryResultCode != 2) {
                                    // Payment failed (not pending; ZaloPay uses 2 for pending)
                                    log.warn("Payment failed with return_code: {} for appTransId: {}", queryResultCode, appTransId);
                                    break;
                                }
                                // If return_code is 2 (pending), continue retrying
                            } else {
                                log.warn("ZaloPay query response data is null for appTransId: {}", appTransId);
                            }
                        } else {
                            log.warn("ZaloPay query failed or returned unsuccessful response for appTransId: {}", appTransId);
                        }
                    } catch (Exception e) {
                        log.warn("Error querying ZaloPay status for appTransId {} (attempt {}): {}", appTransId, i + 1, e.getMessage());
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
            log.error("Error processing ZaloPay return: {}", e.getMessage(), e);
            StringBuilder redirectUrl = new StringBuilder("redirect:/guest/failure-thanhtoan");
            redirectUrl.append("?errorMessage=").append(URLEncoder.encode("Lỗi xử lý thanh toán: " + e.getMessage(), StandardCharsets.UTF_8));
            appendRedirectParams(redirectUrl, invoiceId, roomId, hostelId, addressId);
            return redirectUrl.toString();
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
    private String generateHmacSHA256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
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

    /**
     * Get current date in ZaloPay format (yyMMdd)
     */
    private String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
    }
}