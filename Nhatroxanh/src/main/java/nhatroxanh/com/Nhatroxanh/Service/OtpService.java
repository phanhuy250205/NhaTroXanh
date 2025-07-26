
package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.entity.Users;

public interface OtpService {
   // Tạo và gửi OTP cho một user cụ thể
    void createAndSendOtp(Users user);

    // Xác thực OTP mà người dùng cung cấp
    boolean verifyOtp(Users user, String providedOtp);
    
    // Tạo và gửi OTP cho giao dịch rút tiền
    void createAndSendWithdrawalOtp(Users user, Double amount);
    
    // Xác thực OTP cho giao dịch rút tiền
    boolean verifyWithdrawalOtp(Users user, String providedOtp);
}
