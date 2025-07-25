package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
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
    public void createAndSendOtp(Users user) {
        String otpCode = String.valueOf(100000 + new SecureRandom().nextInt(900000));
        user.setOtpCode(otpCode);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(OTP_VALID_DURATION_MINUTES));
        userRepository.save(user);

        String title = "Mã OTP Của Bạn";
        String greeting = "Xin chào " + user.getFullname();
        String content = String.format(
                """
                        <div style="max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif; color: #333333;">
                            <div style="text-align: center; margin-bottom: 30px;">
                                <h2 style="color: %s;">Mã OTP Của Bạn</h2>
                            </div>

                            <p>Xin cảm ơn bạn đã sử dụng dịch vụ của <strong>Nhà Trọ Xanh</strong>.</p>
                            <p>Vui lòng sử dụng mã OTP sau để hoàn tất thủ tục xác thực:</p>
                            <p style="font-size: 13px; color: #666666;">Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với người khác, bao gồm cả nhân viên Nhà Trọ Xanh.</p>

                            <div style="text-align: center; margin: 30px 0;">
                                <div style="font-size: 32px; letter-spacing: 10px; font-weight: bold; color: %s;">
                                    %s
                                </div>
                            </div>

                            <div style="border-top: 1px solid #eeeeee; padding-top: 20px; margin-top: 30px;">
                                <p style="font-size: 13px; color: #666666;">Cần hỗ trợ? Liên hệ <a href="mailto:support@nhatroxanh.com" style="color: %s;">support@nhatroxanh.com</a> hoặc truy cập Trung tâm hỗ trợ của chúng tôi</p>
                            </div>
                        </div>
                        """,
                PRIMARY_COLOR,
                PRIMARY_COLOR,
                otpCode,
                PRIMARY_COLOR);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("nhatroxanh123@gmail.com", "Nhà Trọ Xanh");
            helper.setTo(user.getEmail());
            helper.setSubject("Mã OTP của bạn - Nhà Trọ Xanh");
            helper.setText(content, true);

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể gửi email OTP: " + e.getMessage());
        }
    }

    @Override
    public void createAndSendWithdrawalOtp(Users user, Double amount) {
        String otpCode = String.valueOf(100000 + new SecureRandom().nextInt(900000));
        user.setOtpCode(otpCode);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(OTP_VALID_DURATION_MINUTES));
        userRepository.save(user);

        // Format amount to Vietnamese currency
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
                    <p style="color: %s;">(Mã có hiệu lực trong 5 phút)</p>
                </div>

                <div class="warning">
                    <p>⚠️ CẢNH BÁO BẢO MẬT: Không chia sẻ mã này với bất kỳ ai!</p>
                    <p>%s sẽ không bao giờ yêu cầu bạn cung cấp mã OTP.</p>
                </div>

                <p>Nếu bạn không thực hiện giao dịch này, vui lòng liên hệ hỗ trợ ngay lập tức.</p>
                """,
                COMPANY_NAME,
                formattedAmount,
                new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date()),
                otpCode,
                WARNING_COLOR,
                COMPANY_NAME);

        sendHtmlEmail(user.getEmail(), title, content, greeting,
                "Bảo mật tài khoản của bạn là ưu tiên hàng đầu của chúng tôi");
    }

    private void sendHtmlEmail(String to, String subject, String content,
            String greeting, String footerNote) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("nhatroxanh123@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(getEmailTemplate(subject, content, greeting, footerNote), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể gửi email OTP: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyOtp(Users user, String providedOtp) {
        if (isValidOtp(user, providedOtp)) {
            user.setEnabled(true);
            clearOtp(user);
            return true;
        }
        return false;
    }

    @Override
    public boolean verifyWithdrawalOtp(Users user, String providedOtp) {
        if (isValidOtp(user, providedOtp)) {
            clearOtp(user);
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
}