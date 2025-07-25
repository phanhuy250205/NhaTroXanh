package nhatroxanh.com.Nhatroxanh.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.PaymentsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final PaymentsRepository paymentsRepository;
    private final NotificationService notificationService;
    private final WalletService walletService;

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    // VNPay Configuration from environment variables
    @Value("${vnpay.tmnCode:DEMO}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret:DEMO}")
    private String vnpHashSecret;

    @Value("${vnpay.payUrl:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl:http://localhost:8082/vnpay/return}")
    private String vnpReturnUrl;

    @Value("${vnpay.apiUrl:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String vnpApiUrl;

    @Value("${vnpay.version:2.1.0}")
    private String vnpVersion;

    @Value("${vnpay.command:pay}")
    private String vnpCommand;

    @Value("${vnpay.orderType:other}")
    private String vnpOrderType;

    /**
     * Payment Response class
     */
    public static class PaymentResponse {
        private String paymentUrl;
        private String message;
        private int resultCode;

        public PaymentResponse(String paymentUrl, int resultCode, String message) {
            this.paymentUrl = paymentUrl;
            this.resultCode = resultCode;
            this.message = message;
        }

        // Getters
        public String getPaymentUrl() { return paymentUrl; }
        public String getMessage() { return message; }
        public int getResultCode() { return resultCode; }
    }

    /**
     * Create VNPay payment order
     */
    public PaymentResponse createVNPayOrder(String invoiceId) {
        try {
            log.info("Creating VNPay order for invoiceId={} at {}", invoiceId, LocalDateTime.now());

            Integer paymentId = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + invoiceId));

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                throw new IllegalStateException("Hóa đơn đã được thanh toán.");
            }

            // Create VNPay parameters
            String vnpTxnRef = vnpTmnCode + System.currentTimeMillis();
            String vnpOrderInfo = "Thanh toan hoa don #" + invoiceId;
            long vnpAmount = Math.round(payment.getTotalAmount() * 100); // VNPay requires amount in VND cents
            String vnpCurrCode = "VND";
            String vnpLocale = "vn";
            String vnpIpAddr = "127.0.0.1";

            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", vnpVersion);
            vnpParams.put("vnp_Command", vnpCommand);
            vnpParams.put("vnp_TmnCode", vnpTmnCode);
            vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
            vnpParams.put("vnp_CurrCode", vnpCurrCode);
            vnpParams.put("vnp_TxnRef", vnpTxnRef);
            vnpParams.put("vnp_OrderInfo", vnpOrderInfo);
            vnpParams.put("vnp_OrderType", vnpOrderType);
            vnpParams.put("vnp_Locale", vnpLocale);
            vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
            vnpParams.put("vnp_IpAddr", vnpIpAddr);
            vnpParams.put("vnp_CreateDate", getCurrentDateTime());

            // Build query string and hash
            String queryString = buildQueryString(vnpParams);
            String vnpSecureHash = hmacSHA512(vnpHashSecret, queryString);
            String paymentUrl = vnpPayUrl + "?" + queryString + "&vnp_SecureHash=" + vnpSecureHash;

            // Save transaction reference to payment
            payment.setAppTransId(vnpTxnRef);
            paymentsRepository.save(payment);

            log.info("VNPay order created successfully: paymentUrl={}, txnRef={}", paymentUrl, vnpTxnRef);
            return new PaymentResponse(paymentUrl, 0, "Success");

        } catch (Exception e) {
            log.error("Error creating VNPay order for invoiceId {}: {}", invoiceId, e.getMessage(), e);
            return new PaymentResponse(null, -1, "Failed to create payment request: " + e.getMessage());
        }
    }

    /**
     * Handle VNPay return URL
     */
    public String handlePaymentReturn(Map<String, String> vnpParams, Integer roomId, Integer hostelId, 
                                    Integer addressId, Model model) {
        try {
            String vnpTxnRef = vnpParams.get("vnp_TxnRef");
            String vnpResponseCode = vnpParams.get("vnp_ResponseCode");
            String vnpTransactionStatus = vnpParams.get("vnp_TransactionStatus");
            String vnpSecureHash = vnpParams.get("vnp_SecureHash");
            
            // Check for host wallet context
            String context = vnpParams.get("context");
            String userId = vnpParams.get("user_id");

            log.info("VNPay return with txnRef={}, responseCode={}, transactionStatus={}, context={}, user_id={}", 
                    vnpTxnRef, vnpResponseCode, vnpTransactionStatus, context, userId);

            // If this is a host wallet deposit, redirect to host wallet controller
            if ("host_wallet".equals(context)) {
                log.info("Redirecting host wallet VNPay return to HostWalletController");
                StringBuilder redirectUrl = new StringBuilder("redirect:/host/wallet/vnpay-return?");
                for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                    redirectUrl.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
                // Remove the last "&"
                if (redirectUrl.length() > 0 && redirectUrl.charAt(redirectUrl.length() - 1) == '&') {
                    redirectUrl.setLength(redirectUrl.length() - 1);
                }
                return redirectUrl.toString();
            }

            // Continue with regular guest payment processing
            // Verify signature
            Map<String, String> fields = new HashMap<>(vnpParams);
            fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            fields.remove("context");
            fields.remove("user_id");
            
            String signValue = buildQueryString(fields);
            String computedHash = hmacSHA512(vnpHashSecret, signValue);

            if (!computedHash.equals(vnpSecureHash)) {
                log.error("Invalid signature for txnRef: {}", vnpTxnRef);
                return buildFailureRedirect("Chữ ký không hợp lệ", roomId, hostelId, addressId);
            }

            // Find payment
            Payments payment = paymentsRepository.findByAppTransId(vnpTxnRef).orElse(null);
            if (payment == null) {
                log.warn("Payment not found for txnRef: {}", vnpTxnRef);
                return buildFailureRedirect("Không tìm thấy thông tin thanh toán", roomId, hostelId, addressId);
            }

            // Check if already processed
            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                log.info("Payment already processed: {}", payment.getId());
                return buildSuccessRedirect(payment.getId(), roomId, hostelId, addressId);
            }

            // Process payment result
            if ("00".equals(vnpResponseCode) && "00".equals(vnpTransactionStatus)) {
                // Payment successful
                payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
                payment.setPaymentMethod(Payments.PaymentMethod.VNPAY);
                payment.setPaymentDate(new java.sql.Date(System.currentTimeMillis()));
                paymentsRepository.save(payment);

                // Add money to landlord's balance and send notification
                try {
                    Contracts contract = payment.getContract();
                    if (contract != null && contract.getTenant() != null) {
                        Users tenant = contract.getTenant();
                        String title = "Thanh toán thành công";
                        String roomName = contract.getRoom() != null ? contract.getRoom().getNamerooms() : "N/A";
                        String hostelName = contract.getRoom() != null && contract.getRoom().getHostel() != null
                                ? contract.getRoom().getHostel().getName() : "N/A";
                        String formattedAmount = CURRENCY_FORMAT.format(payment.getTotalAmount()) + " VNĐ";

                        String notificationMessage = String.format(
                                "Bạn đã thanh toán thành công hóa đơn #%d cho phòng %s tại %s. Số tiền: %s. Phương thức: VNPay. Cảm ơn bạn đã thanh toán đúng hạn!",
                                payment.getId(), roomName, hostelName, formattedAmount);

                        notificationService.handlePaymentSuccess(tenant, payment, title, notificationMessage);
                        log.info("Payment success notification handled for payment: {}", payment.getId());
                        
                        // Add payment amount to landlord's balance
                        try {
                            Users updatedLandlord = walletService.addPaymentToLandlordBalance(payment);
                            log.info("Successfully added payment amount {} to landlord {} balance for VNPay payment {}", 
                                    formattedAmount, updatedLandlord.getUserId(), payment.getId());
                        } catch (Exception walletException) {
                            log.error("Failed to add payment to landlord balance for VNPay payment {}: {}", 
                                    payment.getId(), walletException.getMessage(), walletException);
                            // Don't fail the entire payment process if wallet update fails
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to send notification for payment {}: {}", payment.getId(), e.getMessage());
                }

                log.info("VNPay payment processed successfully: paymentId={}, txnRef={}", payment.getId(), vnpTxnRef);
                return buildSuccessRedirect(payment.getId(), roomId, hostelId, addressId);
            } else {
                // Payment failed
                log.warn("VNPay payment failed for txnRef: {}, responseCode: {}", vnpTxnRef, vnpResponseCode);
                String errorMessage = getVNPayErrorMessage(vnpResponseCode);
                return buildFailureRedirect(errorMessage, payment.getId(), roomId, hostelId, addressId);
            }

        } catch (Exception e) {
            log.error("Error processing VNPay return: {}", e.getMessage(), e);
            return buildFailureRedirect("Lỗi xử lý thanh toán: " + e.getMessage(), roomId, hostelId, addressId);
        }
    }

    /**
     * Build query string from parameters (sorted by key)
     */
    private String buildQueryString(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder sb = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                try {
                    sb.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (UnsupportedEncodingException e) {
                    log.error("Error encoding parameter: {}", e.getMessage());
                }
            }
        }
        return sb.toString();
    }

    /**
     * Generate HMAC SHA512 hash
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC SHA512: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Get current date time in VNPay format
     */
    private String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * Get VNPay error message by response code
     */
    private String getVNPayErrorMessage(String responseCode) {
        switch (responseCode) {
            case "01": return "Giao dịch chưa hoàn tất";
            case "02": return "Giao dịch bị lỗi";
            case "04": return "Giao dịch đảo (Khách hàng đã bị trừ tiền tại Ngân hàng nhưng GD chưa thành công ở VNPAY)";
            case "05": return "VNPAY đang xử lý giao dịch này (GD hoàn tiền)";
            case "06": return "VNPAY đã gửi yêu cầu hoàn tiền sang Ngân hàng (GD hoàn tiền)";
            case "07": return "Giao dịch bị nghi ngờ gian lận";
            case "09": return "GD Hoàn trả bị từ chối";
            case "10": return "Đã giao hàng";
            case "20": return "Đã thu tiền khách hàng";
            case "21": return "Giao dịch chưa được thanh toán";
            case "22": return "Giao dịch bị hủy";
            default: return "Giao dịch thất bại";
        }
    }

    /**
     * Build success redirect URL
     */
    private String buildSuccessRedirect(Integer paymentId, Integer roomId, Integer hostelId, Integer addressId) {
        StringBuilder redirectUrl = new StringBuilder("redirect:/guest/success-thanhtoan?invoiceId=" + paymentId);
        appendRedirectParams(redirectUrl, null, roomId, hostelId, addressId);
        return redirectUrl.toString();
    }

    /**
     * Build failure redirect URL
     */
    private String buildFailureRedirect(String errorMessage, Integer roomId, Integer hostelId, Integer addressId) {
        StringBuilder redirectUrl = new StringBuilder("redirect:/guest/failure-thanhtoan");
        redirectUrl.append("?errorMessage=").append(URLEncoder.encode(errorMessage, StandardCharsets.UTF_8));
        appendRedirectParams(redirectUrl, null, roomId, hostelId, addressId);
        return redirectUrl.toString();
    }

    /**
     * Build failure redirect URL with payment ID
     */
    private String buildFailureRedirect(String errorMessage, Integer paymentId, Integer roomId, Integer hostelId, Integer addressId) {
        StringBuilder redirectUrl = new StringBuilder("redirect:/guest/failure-thanhtoan?invoiceId=" + paymentId);
        redirectUrl.append("&errorMessage=").append(URLEncoder.encode(errorMessage, StandardCharsets.UTF_8));
        appendRedirectParams(redirectUrl, null, roomId, hostelId, addressId);
        return redirectUrl.toString();
    }

    /**
     * Append redirect parameters
     */
    private void appendRedirectParams(StringBuilder redirectUrl, String prefix, Integer roomId, Integer hostelId, Integer addressId) {
        if (roomId != null) {
            redirectUrl.append("&room_id=").append(roomId);
        }
        if (hostelId != null) {
            redirectUrl.append("&hostel_id=").append(hostelId);
        }
        if (addressId != null) {
            redirectUrl.append("&address_id=").append(addressId);
        }
    }
}
