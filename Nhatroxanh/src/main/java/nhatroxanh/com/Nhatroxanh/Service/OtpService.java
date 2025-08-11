
package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.entity.Users;

public interface OtpService {


    
    // Tạo và gửi OTP cho giao dịch rút tiền
    void createAndSendWithdrawalOtp(Users user, Double amount);
    
    // Xác thực OTP cho giao dịch rút tiền
    boolean verifyWithdrawalOtp(Users user, String providedOtp);

    String generateOtp();
    void sendVerificationEmail(String toEmail, String fullName, String otp);
    void createAndSendPasswordResetOtp(Users user);

    boolean verifyPasswordResetOtp(Users user, String otp);
}
