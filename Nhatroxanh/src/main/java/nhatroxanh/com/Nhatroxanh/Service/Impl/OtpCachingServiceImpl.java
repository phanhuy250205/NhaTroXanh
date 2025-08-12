package nhatroxanh.com.Nhatroxanh.Service.Impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import nhatroxanh.com.Nhatroxanh.Service.OtpCachingService;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class OtpCachingServiceImpl implements OtpCachingService {

    private final Cache<String, UserRequest> registrationCache;
    private final Cache<String, String> otpCache;
    private final Cache<String, String> passwordResetTokenCache;
    
    public OtpCachingServiceImpl() {
        // Cấu hình cache cho đăng ký
        this.registrationCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();

        // Cấu hình cache cho OTP đăng ký
        this.otpCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();

        // <<< BẠN ĐANG THIẾU ĐOẠN CODE NÀY >>>
        // Cấu hình cache cho token quên mật khẩu
        this.passwordResetTokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES) // Token sống trong 10 phút
                .maximumSize(1000)
                .build();
    }

    @Override
    public void cacheData(String key, UserRequest data, String otp) {
        registrationCache.put(key, data);
        otpCache.put(key, otp);
    }
    
    @Override
    public UserRequest getCachedData(String key) {
        return registrationCache.getIfPresent(key);
    }

    @Override
    public String getCachedOtp(String key) {
        return otpCache.getIfPresent(key);
    }
    
    @Override
    public void clearCache(String key) {
        registrationCache.invalidate(key);
        otpCache.invalidate(key);
    }

    @Override
    public void cachePasswordResetToken(String token, String email) {
        this.passwordResetTokenCache.put(token, email);
    }
    
    // Cần thêm 2 phương thức còn lại của interface
    @Override
    public String getPasswordResetToken(String token) {
        return this.passwordResetTokenCache.getIfPresent(token);
    }

    @Override
    public void clearPasswordResetToken(String token) {
        this.passwordResetTokenCache.invalidate(token);
    }
}