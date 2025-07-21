package nhatroxanh.com.Nhatroxanh.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Transaction;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletPaymentService {

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final BankAccountService bankAccountService;

    // VietQR Configuration - Using correct img.vietqr.io endpoint
    @Value("${vietqr.apiUrl:https://img.vietqr.io/image}")
    private String vietQRApiUrl;

    /**
     * Payment Response class for wallet deposits
     */
    public static class WalletPaymentResponse {
        private String paymentUrl;
        private String qrCodeUrl;
        private String message;
        private int resultCode;
        private String transactionId;

        public WalletPaymentResponse(String paymentUrl, String qrCodeUrl, int resultCode, String message, String transactionId) {
            this.paymentUrl = paymentUrl;
            this.qrCodeUrl = qrCodeUrl;
            this.resultCode = resultCode;
            this.message = message;
            this.transactionId = transactionId;
        }

        // Getters
        public String getPaymentUrl() { return paymentUrl; }
        public String getQrCodeUrl() { return qrCodeUrl; }
        public String getMessage() { return message; }
        public int getResultCode() { return resultCode; }
        public String getTransactionId() { return transactionId; }
    }

    /**
     * Process VietQR payment for wallet deposit
     */
    public WalletPaymentResponse processVietQRWalletDeposit(Users user, Double amount, String description) {
        try {
            // Validate input
            if (user == null || amount == null || amount <= 0) {
                log.error("Invalid input: user is null or amount is invalid (amount={})", amount);
                return new WalletPaymentResponse(null, null, -1, "Dữ liệu đầu vào không hợp lệ", null);
            }

            // Create transaction for wallet deposit
            Transaction transaction = transactionService.createDepositRequest(
                user, amount, "vietqr", description != null ? description : "Nạp tiền qua VietQR");

            // Generate unique transaction reference
            String transactionRef = "WALLET" + System.currentTimeMillis();
            transaction.setTransactionReference(transactionRef);

            // Create VietQR payment content with better formatting
            String paymentContent = "NAP TIEN " + transaction.getTransactionId() + " " + 
                (user.getFullname() != null ? user.getFullname().replaceAll("[^a-zA-Z0-9 ]", "") : "USER");
            
            // Generate VietQR URL with proper error handling
            String qrCodeUrl;
            try {
                qrCodeUrl = generateVietQRUrl(amount, paymentContent);
                log.info("Successfully generated VietQR URL for transaction {}", transaction.getTransactionId());
            } catch (Exception qrError) {
                log.error("Failed to generate VietQR URL for transaction {}: {}", transaction.getTransactionId(), qrError.getMessage());
                transactionService.failTransaction(transaction.getTransactionId(), "Lỗi tạo mã QR: " + qrError.getMessage());
                return new WalletPaymentResponse(null, null, -1, "Không thể tạo mã QR thanh toán: " + qrError.getMessage(), null);
            }
            
            // For VietQR, we don't have a direct payment URL, so we'll use a confirmation page
            String paymentUrl = "/host/wallet/vietqr-confirm?transactionId=" + transaction.getTransactionId();

            log.info("Created VietQR wallet deposit order for transaction {} with amount {}", 
                transaction.getTransactionId(), amount);
            log.info("VietQR payment content: {}", paymentContent); 

            return new WalletPaymentResponse(paymentUrl, qrCodeUrl, 0, "Success", transaction.getTransactionId().toString());

        } catch (Exception e) {
            log.error("Error processing VietQR wallet deposit for user {}: {}", user.getUserId(), e.getMessage(), e);
            return new WalletPaymentResponse(null, null, -1, "Lỗi xử lý yêu cầu nạp tiền: " + e.getMessage(), null);
        }
    }

    /**
     * Generate VietQR URL for payment using staff bank account
     */
    private String generateVietQRUrl(Double amount, String content) {
        try {
            log.info("Starting VietQR URL generation for amount: {} with content: {}", amount, content);
            
            // Validate VietQR API URL
            if (vietQRApiUrl == null || vietQRApiUrl.trim().isEmpty()) {
                log.error("VietQR API URL is not configured properly");
                throw new RuntimeException("URL API VietQR không được cấu hình");
            }

            // Get staff bank account information with retry mechanism
            BankAccountService.BankAccountInfo bankInfo = null;
            int retryCount = 0;
            int maxRetries = 3;
            
            while (bankInfo == null && retryCount < maxRetries) {
                try {
                    bankInfo = bankAccountService.getStaffBankAccount();
                    if (bankInfo != null) {
                        log.info("Successfully retrieved bank account info on attempt {}", retryCount + 1);
                        break;
                    }
                } catch (Exception e) {
                    log.warn("Attempt {} to get bank account failed: {}", retryCount + 1, e.getMessage());
                }
                retryCount++;
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(100); // Short delay before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // Validate bank information with detailed error messages
            if (bankInfo == null) {
                log.error("Bank account information is null after {} attempts", maxRetries);
                throw new RuntimeException("Không thể lấy thông tin tài khoản ngân hàng. Vui lòng liên hệ quản trị viên để cấu hình tài khoản ngân hàng.");
            }
            
            if (bankInfo.getBankId() == null || bankInfo.getBankId().trim().isEmpty()) {
                log.error("Bank ID is null or empty: {}", bankInfo.getBankId());
                throw new RuntimeException("Mã ngân hàng không hợp lệ. Vui lòng liên hệ quản trị viên.");
            }
            
            if (bankInfo.getAccountNo() == null || bankInfo.getAccountNo().trim().isEmpty()) {
                log.error("Account number is null or empty: {}", bankInfo.getAccountNo());
                throw new RuntimeException("Số tài khoản không hợp lệ. Vui lòng liên hệ quản trị viên.");
            }
            
            if (bankInfo.getAccountHolderName() == null || bankInfo.getAccountHolderName().trim().isEmpty()) {
                log.error("Account holder name is null or empty: {}", bankInfo.getAccountHolderName());
                throw new RuntimeException("Tên chủ tài khoản không hợp lệ. Vui lòng liên hệ quản trị viên.");
            }
            
            // Convert bank ID to numeric format
            String numericBankId;
            try {
                numericBankId = convertToNumericBankId(bankInfo.getBankId().trim());
                log.info("Converted bank ID {} to numeric format: {}", bankInfo.getBankId(), numericBankId);
            } catch (Exception e) {
                log.error("Error converting bank ID {}: {}", bankInfo.getBankId(), e.getMessage());
                throw new RuntimeException("Lỗi chuyển đổi mã ngân hàng: " + e.getMessage());
            }
            
            // Validate and format amount
            if (amount == null || amount <= 0) {
                throw new RuntimeException("Số tiền không hợp lệ");
            }
            long amountLong = Math.round(amount);
            
            // Clean and validate content
            String cleanContent = content;
            if (cleanContent == null || cleanContent.trim().isEmpty()) {
                cleanContent = "NAP TIEN";
            }
            cleanContent = cleanContent.replaceAll("[^a-zA-Z0-9 ]", "").trim();
            if (cleanContent.length() > 25) {
                cleanContent = cleanContent.substring(0, 25);
            }
            
            // Build VietQR URL - Simplified approach
            // Format: https://img.vietqr.io/image/{bankId}-{accountNo}-compact.png?amount={amount}&addInfo={content}&accountName={name}
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(vietQRApiUrl);
            
            // Ensure URL ends with proper separator
            if (!vietQRApiUrl.endsWith("/")) {
                urlBuilder.append("/");
            }
            
            // Add bank ID and account number
            urlBuilder.append(numericBankId)
                     .append("-")
                     .append(bankInfo.getAccountNo().trim())
                     .append("-compact.png");
            
            // Add query parameters
            urlBuilder.append("?amount=").append(amountLong);
            urlBuilder.append("&addInfo=").append(URLEncoder.encode(cleanContent, StandardCharsets.UTF_8));
            urlBuilder.append("&accountName=").append(URLEncoder.encode(bankInfo.getAccountHolderName().trim(), StandardCharsets.UTF_8));
            
            String finalUrl = urlBuilder.toString();
            
            log.info("Generated VietQR URL using bank account: {} - Bank ID: {} -> {} - Account: {}", 
                bankInfo.getBankName(), bankInfo.getBankId(), numericBankId,
                bankInfo.getAccountNo().substring(0, Math.min(4, bankInfo.getAccountNo().length())) + "****");
            log.info("VietQR URL generated successfully with length: {}", finalUrl.length());
            
            // Validate URL length
            if (finalUrl.length() > 2000) {
                log.warn("Generated URL is very long ({}), might cause issues", finalUrl.length());
            }
            
            return finalUrl;

        } catch (Exception e) {
            log.error("Error generating VietQR URL: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tạo mã QR: " + e.getMessage());
        }
    }

    /**
     * Convert bank ID from short code to numeric format for VietQR
     */
    private String convertToNumericBankId(String bankId) {
        if (bankId == null || bankId.trim().isEmpty()) {
            log.warn("Bank ID is null or empty, using default");
            return "970436"; // Default to Vietcombank
        }
        
        String trimmedBankId = bankId.trim().toUpperCase();
        
        // If already numeric format, return as is
        if (trimmedBankId.matches("^\\d{6}$")) {
            log.info("Bank ID {} is already in numeric format", trimmedBankId);
            return trimmedBankId;
        }
        
        // Convert short codes to numeric format
        switch (trimmedBankId) {
            case "MB": return "970422"; // MB Bank
            case "VCB": case "VIETCOMBANK": return "970436"; // Vietcombank
            case "BIDV": return "970418"; // BIDV
            case "VTB": case "VIETINBANK": return "970415"; // Vietinbank
            case "ACB": return "970416"; // ACB
            case "TCB": case "TECHCOMBANK": return "970407"; // Techcombank
            case "VPB": case "VPBANK": return "970432"; // VPBank
            case "TPB": case "TPBANK": return "970423"; // TPBank
            case "STB": case "SACOMBANK": return "970403"; // Sacombank
            case "HDB": case "HDBANK": return "970437"; // HDBank
            case "VIB": return "970441"; // VIB
            case "SHB": return "970443"; // SHB
            case "EIB": case "EXIMBANK": return "970431"; // Eximbank
            case "MSB": return "970426"; // MSB
            case "SEAB": case "SEABANK": return "970440"; // SeABank
            case "OCB": return "970448"; // OCB
            case "LPB": case "LIENVIETPOSTBANK": return "970439"; // LienVietPostBank
            case "ABB": case "ABBANK": return "970425"; // ABBank
            case "VAB": case "VIETNAMABANK": return "970427"; // VietABank
            case "NAB": case "NAMABANK": return "970428"; // Nam A Bank
            case "PGB": case "PGBANK": return "970430"; // PGBank
            case "AGRI": case "AGRIBANK": return "970405"; // Agribank
            case "GPB": case "GPBANK": return "970408"; // GPBank
            case "BACA": case "BACABANK": return "970409"; // BacA Bank
            case "PVC": case "PVCOMBANK": return "970412"; // PVcomBank
            case "OJB": case "OCEANBANK": return "970414"; // OceanBank
            case "SHBVN": case "SHINHAN": return "970424"; // Shinhan Bank
            case "SCB": return "970429"; // SCB
            case "VIETBANK": return "970433"; // VietBank
            case "BVB": case "BAOVIETBANK": return "970468"; // BaoVietBank
            case "COOPBANK": return "970467"; // COOPBANK
            case "KLB": case "KIENLONGBANK": return "970466"; // KienLongBank
            case "NCB": return "970469"; // NCB
            case "UOB": return "970458"; // UOB Vietnam
            case "HSBC": return "970463"; // HSBC Vietnam
            case "IBK": return "970455"; // IBK - HCMC
            case "CAKE": return "970456"; // CAKE by VPBank
            case "VIETCAP": case "VIETCAPITALBANK": return "970454"; // VietCapitalBank
            default:
                log.warn("Unknown bank ID: {}, using default Vietcombank", bankId);
                return "970436"; // Default to Vietcombank
        }
    }

    /**
     * Handle successful wallet deposit
     */
    public void handleSuccessfulWalletDeposit(String transactionId, Users user) {
        try {
            // Get transaction by ID
            Optional<Transaction> transactionOpt = transactionService.getTransactionById(Integer.parseInt(transactionId));
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                if (transaction.getStatus() == Transaction.TransactionStatus.PENDING) {
                    // Only mark payment as confirmed - DO NOT add balance yet
                    transaction.setProcessedAt(LocalDateTime.now());
                    transaction.setDescription(transaction.getDescription() + " - Đã thanh toán, chờ duyệt");
                    
                    log.info("Payment confirmed for transaction {} from user {}. Waiting for staff approval before adding balance.", 
                            transactionId, user.getUserId());
                }
            } else {
                log.warn("Transaction not found with ID: {}", transactionId);
            }
        } catch (Exception e) {
            log.error("Error handling successful wallet deposit for transaction {}: {}", transactionId, e.getMessage());
            throw new RuntimeException("Failed to update transaction status", e);
        }
    }

    /**
     * Build query string from parameters
     */
    private String buildQueryString(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        
        StringJoiner sj = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    sj.add(fieldName + "=" + URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (UnsupportedEncodingException e) {
                    log.error("Error encoding parameter {}: {}", fieldName, e.getMessage());
                }
            }
        }
        return sj.toString();
    }

    /**
     * Get current date-time string
     */
    private String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}