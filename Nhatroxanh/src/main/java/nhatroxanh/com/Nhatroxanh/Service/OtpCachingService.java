// File: nhatroxanh/com/Nhatroxanh/Service/OtpCachingService.java

package nhatroxanh.com.Nhatroxanh.Service;


import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import org.springframework.stereotype.Service;


// Dùng để lưu trữ tạm thời thông tin đăng ký và OTP
@Service
public interface OtpCachingService {

    void cacheData(String key, UserRequest data, String otp);

    UserRequest getCachedData(String key);

    String getCachedOtp(String key);
    
    void clearCache(String key);
    void cachePasswordResetToken(String token, String email);
     String getPasswordResetToken(String token);
    void clearPasswordResetToken(String token);
}