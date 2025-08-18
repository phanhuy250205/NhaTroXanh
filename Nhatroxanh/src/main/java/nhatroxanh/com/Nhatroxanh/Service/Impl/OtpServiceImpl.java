package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

@Service
public class OtpServiceImpl implements OtpService {

    private static final String PRIMARY_COLOR = "#3498DB";
    private static final String SECONDARY_COLOR = "#F8F9FA";
    private static final String TEXT_COLOR = "#333333";
    private static final String WARNING_COLOR = "#E74C3C";
    private static final String COMPANY_NAME = "Nhà Trọ Xanh";
    private static final long OTP_VALID_DURATION_MINUTES = 5;

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private UserRepository userRepository;

    private String getEmailTemplate(String title, String content, String greeting, String footerNote) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                body { font-family: 'Arial', sans-serif; line-height: 1.6; color: %s; margin: 0; padding: 0; }
                                .container { max-width: 600px; margin: 0 auto; background: white; }
                                .header { background-color: %s; padding: 25px; text-align: center; }
                                .header h1 { color: white; margin: 0; font-size: 22px; }
                                .content { padding: 30px; background-color: %s; }
                                .footer { padding: 15px; text-align: center; font-size: 12px; color: #7F8C8D; }
                                .highlight { background-color: #EAF2F8; padding: 15px; border-left: 3px solid %s; margin: 20px 0; }
                                .otp-code { background: %s; color: white; padding: 15px; font-size: 28px;
                                            letter-spacing: 3px; font-weight: bold; display: inline-block;
                                            border-radius: 5px; margin: 10px 0; }
                                .warning { color: %s; font-weight: bold; background: #FDEDEC; padding: 10px;
                                          border-radius: 4px; margin: 15px 0; }
                                .contact-info { margin-top: 20px; font-size: 14px; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>%s</h1>
                                </div>
                                <div class="content">
                                    <p>%s,</p>
                                    %s
                                    <div class="contact-info">
                                        <p>Nếu bạn cần hỗ trợ, vui lòng liên hệ:</p>
                                        <p>Email: support@nhatroxanh.com | Hotline: 1900.1234</p>
                                    </div>
                                    <p>Trân trọng,<br>Đội ngũ %s</p>
                                </div>
                                <div class="footer">
                                    <p>%s</p>
                                    <p>© %d %s. All rights reserved.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                TEXT_COLOR, PRIMARY_COLOR, SECONDARY_COLOR, PRIMARY_COLOR,
                PRIMARY_COLOR, WARNING_COLOR, title, greeting, content,
                COMPANY_NAME, footerNote, new Date().getYear() + 1900, COMPANY_NAME);
    }

    @Override
    public void createAndSendWithdrawalOtp(Users user, Double amount) {
        String otpCode = generateOtp(); // Tái sử dụng phương thức tạo OTP
        user.setOtpCode(otpCode);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(OTP_VALID_DURATION_MINUTES));
        userRepository.save(user);

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedAmount = formatter.format(amount) + " VNĐ";

        String title = "Mã OTP Xác Thực Rút Tiền";
        String greeting = "Xin chào " + user.getFullname();
        String content = String.format("""
                <p>Bạn đang thực hiện giao dịch rút tiền từ tài khoản %s.</p>
                <div class="highlight">
                    <p><strong>Số tiền:</strong> %s</p>
                    <p><strong>Thời gian yêu cầu:</strong> %s</p>
                </div>
                <div style="text-align: center; margin: 25px 0;">
                    <p>Vui lòng nhập mã OTP sau để xác nhận giao dịch:</p>
                    <div class="otp-code">%s</div>
                </div>
                """, COMPANY_NAME, formattedAmount, new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date()),
                otpCode);

        sendHtmlEmail(user.getEmail(), title, content, greeting,
                "Bảo mật tài khoản của bạn là ưu tiên hàng đầu của chúng tôi");
    }

    private void sendHtmlEmail(String to, String subject, String content, String greeting, String footerNote) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("nhatroxanh123@gmail.com", "Nhà Trọ Xanh");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(getEmailTemplate(subject, content, greeting, footerNote), true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWithdrawalOtp(Users user, String providedOtp) {
        if (isValidOtp(user, providedOtp)) {
            clearOtp(user); // <<< LỖI CỦA BẠN LÀ DO PHƯƠNG THỨC NÀY BỊ THIẾU
            return true;
        }
        return false;
    }

    private boolean isValidOtp(Users user, String providedOtp) {
        return user.getOtpCode() != null &&
                user.getOtpCode().equals(providedOtp) &&
                user.getOtpExpiration() != null &&
                user.getOtpExpiration().isAfter(LocalDateTime.now());
    }

    private void clearOtp(Users user) {
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);
    }

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String otp) {
        String title = "Mã OTP Xác Thực Tài Khoản";
        String greeting = "Xin chào " + fullName;
        String content = String.format(
                """
                        <p>Cảm ơn bạn đã đăng ký tài khoản tại Nhà Trọ Xanh.</p>
                        <p>Vui lòng sử dụng mã OTP dưới đây để hoàn tất việc xác thực. Mã có hiệu lực trong 5 phút.</p>
                        <div style="text-align: center; margin: 30px 0;">
                           <div style="font-size: 32px; letter-spacing: 10px; font-weight: bold; color: %s;">
                                %s
                           </div>
                        </div>
                        """, PRIMARY_COLOR, otp);

        sendHtmlEmail(toEmail, title, content, greeting, "Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.");
    }

    @Override
    public String generateOtp() {
        // Tạo một số ngẫu nhiên có 6 chữ số (từ 100000 đến 999999)
        return String.valueOf(100000 + new java.security.SecureRandom().nextInt(900000));
    }

    @Override
    public void createAndSendPasswordResetOtp(Users user) {
        // 1. Tạo mã OTP ngẫu nhiên
        String otpCode = generateOtp();

        // 2. Lưu mã OTP và thời gian hết hạn vào đối tượng user
        user.setOtpCode(otpCode);
        user.setOtpExpiration(java.time.LocalDateTime.now().plusMinutes(5)); // Hết hạn sau 5 phút

        // 3. Cập nhật thông tin user vào database
        userRepository.save(user);

        // 4. Gửi email chứa mã OTP
        sendResetPasswordEmail(user.getEmail(), user.getFullname(), otpCode);
    }

    @Override
    public boolean verifyPasswordResetOtp(Users user, String otp) {
        // 1. Kiểm tra xem OTP có khớp và còn hạn hay không
        boolean isValid = user.getOtpCode() != null &&
                user.getOtpCode().equals(otp) &&
                user.getOtpExpiration() != null &&
                user.getOtpExpiration().isAfter(java.time.LocalDateTime.now());

        // 2. Nếu OTP hợp lệ, xóa nó khỏi database để tránh dùng lại
        if (isValid) {
            user.setOtpCode(null);
            user.setOtpExpiration(null);
            userRepository.save(user);
        }

        // 3. Trả về kết quả
        return isValid;
    }

    @Override
    public void sendResetPasswordEmail(String toEmail, String fullName, String otp) {
        String title = "Mã OTP Đặt Lại Mật Khẩu";
        String greeting = "Xin chào " + fullName;
        String content = String.format(
                """
                        <p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản tại Nhà Trọ Xanh.</p>
                        <p>Vui lòng sử dụng mã OTP dưới đây để tiếp tục. Mã có hiệu lực trong 5 phút.</p>
                        <div style="text-align: center; margin: 30px 0;">
                           <div style="font-size: 32px; letter-spacing: 10px; font-weight: bold; color: %s;">
                                %s
                           </div>
                        </div>
                        """, PRIMARY_COLOR, otp);

        sendHtmlEmail(toEmail, title, content, greeting,
                "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email.");
    }

}