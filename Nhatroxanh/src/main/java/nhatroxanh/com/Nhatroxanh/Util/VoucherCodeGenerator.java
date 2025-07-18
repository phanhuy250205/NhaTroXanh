package nhatroxanh.com.Nhatroxanh.Util;

import java.security.SecureRandom;
import java.util.Random;

public class VoucherCodeGenerator {
     private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 10;
    private static final Random RANDOM = new SecureRandom();
    
    /**
     * Tạo mã voucher ngẫu nhiên gồm 10 ký tự (chữ hoa và số)
     * @return Mã voucher duy nhất
     */
    public static String generateVoucherCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        
        return code.toString();
    }
    
    /**
     * Tạo mã voucher với prefix tùy chỉnh
     * @param prefix Tiền tố cho mã voucher
     * @return Mã voucher với prefix
     */
    public static String generateVoucherCodeWithPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return generateVoucherCode();
        }
        
        String randomPart = generateVoucherCode();
        int remainingLength = CODE_LENGTH - prefix.length();
        
        if (remainingLength <= 0) {
            return prefix.substring(0, CODE_LENGTH);
        }
        
        return prefix + randomPart.substring(0, remainingLength);
    }
    
    /**
     * Alias method for backward compatibility
     */
    public static String generateCode() {
        return generateVoucherCode();
    }
    
}