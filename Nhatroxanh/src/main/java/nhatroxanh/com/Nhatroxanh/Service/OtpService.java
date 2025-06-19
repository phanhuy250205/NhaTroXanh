
package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

public interface OtpService {
   // Tạo và gửi OTP cho một user cụ thể
    void createAndSendOtp(Users user);

    // Xác thực OTP mà người dùng cung cấp
    boolean verifyOtp(Users user, String providedOtp);
}