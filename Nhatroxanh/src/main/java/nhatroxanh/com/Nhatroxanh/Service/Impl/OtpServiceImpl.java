package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpServiceImpl implements OtpService {

    @Autowired private JavaMailSender mailSender;
    @Autowired private UserRepository userRepository;

    private static final long OTP_VALID_DURATION_MINUTES = 5;

    @Override
    public void createAndSendOtp(Users user) {
        String otpCode = String.valueOf(100000 + new SecureRandom().nextInt(900000));
        user.setOtpCode(otpCode);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(OTP_VALID_DURATION_MINUTES));
        userRepository.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("nhatroxanh1@gmail.com");
        message.setTo(user.getEmail());
        message.setSubject("Mã xác thực OTP cho tài khoản Nhà Trọ Xanh");
        message.setText("Chào bạn,\n\nMã OTP của bạn là: " + otpCode + "\n\nMã này sẽ hết hạn sau 5 phút.\n\nTrân trọng,\nĐội ngũ Nhà Trọ Xanh.");
        mailSender.send(message);
    }

    @Override
    public boolean verifyOtp(Users user, String providedOtp) {
        if (user.getOtpCode() != null && user.getOtpCode().equals(providedOtp) &&
            user.getOtpExpiration().isAfter(LocalDateTime.now())) {
            user.setEnabled(true);
            user.setOtpCode(null);
            user.setOtpExpiration(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}