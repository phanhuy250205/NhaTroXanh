package nhatroxanh.com.Nhatroxanh.Service.Impl;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import nhatroxanh.com.Nhatroxanh.Model.entity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Model.entity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {

    private static final String PRIMARY_COLOR = "#3498DB";
    private static final String SECONDARY_COLOR = "#F8F9FA";
    private static final String TEXT_COLOR = "#333333";
    private static final String LIGHT_TEXT = "#7F8C8D";
    private static final String COMPANY_NAME = "Nhà Trọ Xanh";

    @Autowired
    private JavaMailSender mailSender;

    private String getEmailTemplate(String title, String content, String greeting, String footer) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                body { font-family: Arial, sans-serif; line-height: 1.6; color: %s; }
                                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                                .header { background-color: %s; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                                .header h1 { color: white; margin: 0; }
                                .content { padding: 30px; background-color: %s; border-left: 1px solid #ddd; border-right: 1px solid #ddd; }
                                .footer { padding: 15px; text-align: center; background-color: %s; border-radius: 0 0 5px 5px; font-size: 12px; color: %s; }
                                .button { background-color: %s; color: white; padding: 10px 15px; text-decoration: none; border-radius: 3px; display: inline-block; }
                                .highlight { background-color: #EAF2F8; padding: 10px; border-left: 3px solid %s; margin: 15px 0; }
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
                                    <p>Trân trọng,<br>Đội ngũ %s</p>
                                </div>
                                <div class="footer">
                                    © %d %s. All rights reserved.
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                TEXT_COLOR, PRIMARY_COLOR, SECONDARY_COLOR, SECONDARY_COLOR, LIGHT_TEXT,
                PRIMARY_COLOR, PRIMARY_COLOR, title, greeting, content, COMPANY_NAME,
                java.time.Year.now().getValue(), COMPANY_NAME);
    }

    @Override
    public void sendExtensionApprovalEmail(String to, String fullname, String contractCode, Date newEndDate) {
        String title = "Gia hạn hợp đồng được phê duyệt";
        String greeting = "Xin chào " + fullname;
        String content = String.format("""
                <p>Yêu cầu gia hạn hợp đồng của bạn đã được phê duyệt thành công.</p>
                <div class="highlight">
                    <p><strong>Mã hợp đồng:</strong> %s</p>
                    <p><strong>Ngày kết thúc mới:</strong> %s</p>
                </div>
                <p>Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của chúng tôi.</p>
                """, contractCode, newEndDate);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendExtensionRejectionEmail(String to, String fullname, String contractCode, String reason) {
        String title = "Yêu cầu gia hạn bị từ chối";
        String greeting = "Xin chào " + fullname;
        String content = String.format("""
                <p>Chúng tôi rất tiếc phải thông báo rằng yêu cầu gia hạn hợp đồng của bạn đã không được chấp thuận.</p>
                <div class="highlight">
                    <p><strong>Mã hợp đồng:</strong> %s</p>
                    <p><strong>Lý do từ chối:</strong> %s</p>
                </div>
                <p>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với bộ phận hỗ trợ của chúng tôi.</p>
                """, contractCode, reason);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendExpirationWarningEmail(String to, String fullname, String contractCode, Date endDate) {
        String title = "Cảnh báo hết hạn hợp đồng";
        String greeting = "Xin chào " + fullname;
        String content = String.format("""
                <p>Hợp đồng thuê của bạn sắp hết hạn. Vui lòng thực hiện gia hạn để tiếp tục sử dụng dịch vụ.</p>
                <div class="highlight">
                    <p><strong>Mã hợp đồng:</strong> %s</p>
                    <p><strong>Ngày hết hạn:</strong> %s</p>
                </div>
                <p>Để gia hạn hợp đồng, vui lòng truy cập trang quản lý hợp đồng trong tài khoản của bạn.</p>
                """, contractCode, endDate);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendReturnApprovalEmail(String to, String fullname, String contractCode, Date endDate) {
        String title = "Yêu cầu trả phòng được duyệt";
        String greeting = "Xin chào " + fullname;
        String content = String.format("""
                <p>Yêu cầu trả phòng của bạn đã được phê duyệt thành công.</p>
                <div class="highlight">
                    <p><strong>Mã hợp đồng:</strong> %s</p>
                    <p><strong>Ngày kết thúc:</strong> %s</p>
                </div>
                <p>Chúng tôi hy vọng bạn đã có những trải nghiệm tốt với dịch vụ của chúng tôi.</p>
                """, contractCode, endDate);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendReturnRejectionEmail(String to, String fullname, String contractCode, String reason) {
        String title = "Yêu cầu trả phòng bị từ chối";
        String greeting = "Xin chào " + fullname;
        String content = String.format("""
                <p>Yêu cầu trả phòng của bạn đã không được chấp thuận.</p>
                <div class="highlight">
                    <p><strong>Mã hợp đồng:</strong> %s</p>
                    <p><strong>Lý do từ chối:</strong> %s</p>
                </div>
                <p>Vui lòng liên hệ với quản lý khu trọ để biết thêm chi tiết.</p>
                """, contractCode, reason);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendContractTerminatedEmail(String to, String fullname, String contractCode, Date endDate) {
        String title = "Hợp đồng đã kết thúc";
        String greeting = "Xin chào " + fullname;
        String content = String.format(
                """
                        <p>Hợp đồng thuê của bạn đã chính thức kết thúc.</p>
                        <div class="highlight">
                            <p><strong>Mã hợp đồng:</strong> %s</p>
                            <p><strong>Ngày kết thúc:</strong> %s</p>
                        </div>
                        <p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi. Chúng tôi hy vọng sẽ được phục vụ bạn trong tương lai.</p>
                        """,
                contractCode, endDate);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendVoucherDeactivatedEmail(String to, String fullname, String voucherTitle, String reason) {
        String title = "Voucher ngừng hoạt động";
        String greeting = "Xin chào " + fullname;
        String content = String.format("""
                <p>Voucher của bạn đã được chuyển sang trạng thái ngừng hoạt động.</p>
                <div class="highlight">
                    <p><strong>Tên voucher:</strong> %s</p>
                    <p><strong>Lý do:</strong> %s</p>
                </div>
                <p>Vui lòng kiểm tra hệ thống để tạo voucher mới nếu cần thiết.</p>
                """, voucherTitle, reason);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendIncidentProcessingEmail(String to, IncidentReports incident) {
        String title = "Sự cố đang được xử lý";
        String greeting = "Xin chào";
        String content = String.format("""
                <p>Sự cố bạn báo cáo đã được tiếp nhận và đang được xử lý.</p>
                <div class="highlight">
                    <p><strong>Loại sự cố:</strong> %s</p>
                    <p><strong>Thời gian xử lý dự kiến:</strong> %s</p>
                </div>
                <p>Chúng tôi sẽ thông báo ngay khi sự cố được giải quyết.</p>
                """, incident.getIncidentType(),
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(incident.getResolvedAt()));

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendIncidentResolvedEmail(String to, IncidentReports incident) {
        String title = "Sự cố đã được giải quyết";
        String greeting = "Xin chào";
        String content = String.format("""
                <p>Sự cố bạn báo cáo đã được giải quyết thành công.</p>
                <div class="highlight">
                    <p><strong>Loại sự cố:</strong> %s</p>
                    <p><strong>Thời gian giải quyết:</strong> %s</p>
                </div>
                <p>Cảm ơn bạn đã thông báo sự cố cho chúng tôi.</p>
                """, incident.getIncidentType(),
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(incident.getResolvedAt()));

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendContractEmail(String to, String customerName, byte[] pdfContent, String contractNumber)
            throws Exception {

        try {
            // ✅ VALIDATE PDF
            if (pdfContent == null || pdfContent.length == 0) {
                throw new Exception("PDF content is null or empty!");
            }

            // ✅ CREATE MESSAGE MANUALLY
            MimeMessage message = mailSender.createMimeMessage();

            // ✅ BASIC HEADERS
            message.setFrom(new InternetAddress("nhatroxanh123@gmail.com"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("🏠 Hợp đồng thuê nhà số " + contractNumber + " - Nhà Trọ Xanh", "UTF-8");

            // ✅ CREATE MULTIPART
            MimeMultipart multipart = new MimeMultipart();

            // ✅ TEXT PART
            MimeBodyPart textPart = new MimeBodyPart();
            String emailBody = buildContractEmailBody(customerName, contractNumber);
            textPart.setContent(emailBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(textPart);

            // ✅ PDF ATTACHMENT PART
            MimeBodyPart attachmentPart = new MimeBodyPart();
            String fileName = "HopDong_" + cleanFileName(contractNumber) + ".pdf";

            ByteArrayDataSource dataSource = new ByteArrayDataSource(pdfContent, "application/pdf");
            dataSource.setName(fileName);

            attachmentPart.setDataHandler(new DataHandler(dataSource));
            attachmentPart.setFileName(fileName);
            attachmentPart.setHeader("Content-Type", "application/pdf");
            attachmentPart.setDisposition(MimeBodyPart.ATTACHMENT);

            multipart.addBodyPart(attachmentPart);

            // ✅ SET CONTENT
            message.setContent(multipart);

            // ✅ SEND
            mailSender.send(message);

        } catch (Exception e) {
            throw e;
        }
    }

    // ✅ CLEAN FILENAME METHOD
    private String cleanFileName(String contractNumber) {
        if (contractNumber == null || contractNumber.trim().isEmpty()) {
            return "Contract_" + System.currentTimeMillis();
        }

        // Remove special characters, keep only alphanumeric, underscore, dash
        String cleaned = contractNumber.replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_{2,}", "_") // Replace multiple underscores with single
                .replaceAll("^_+|_+$", ""); // Remove leading/trailing underscores

        // Limit length
        if (cleaned.length() > 30) {
            cleaned = cleaned.substring(0, 30);
        }

        // Ensure not empty
        if (cleaned.isEmpty()) {
            cleaned = "Contract_" + System.currentTimeMillis();
        }

        return cleaned;
    }

    // ✅ HÀM TẠO NỘI DUNG EMAIL HỢP ĐỒNG
    // ✅ THAY THẾ HÀM buildContractEmailBody BẰNG CODE NÀY
    private String buildContractEmailBody(String customerName, String contractNumber) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        return String.format(
                """
                                <!DOCTYPE html>
                                <html lang="vi">
                                <head>
                                    <meta charset="UTF-8">
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <title>Hợp đồng thuê trọ - Premium</title>
                                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
                                </head>
                                <body style="margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); min-height: 100vh;">
                                   \s
                                    <!-- Main Container -->
                                    <table width="100%%" cellpadding="0" cellspacing="0" style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); min-height: 100vh; padding: 20px 0;">
                                        <tr>
                                            <td align="center">
                                                <!-- Email Card -->
                                                <table width="600" cellpadding="0" cellspacing="0" style="background: #ffffff; border-radius: 20px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); overflow: hidden; max-width: 600px;">
                                                   \s
                                                    <!-- ✨ PREMIUM HEADER -->
                                                    <tr>
                                                        <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center; color: white;">
                                                            <!-- Logo Container -->
                                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                                <tr>
                                                                    <td align="center">
                                                                        <table cellpadding="0" cellspacing="0">
                                                                            <tr>
                                                                                <td style="width: 80px; height: 80px; background: rgba(255,255,255,0.2); border-radius: 50%%; text-align: center; vertical-align: middle; border: 2px solid rgba(255,255,255,0.3);">
                                                                                    <div style="font-size: 32px; line-height: 80px;">🏠</div>
                                                                                </td>
                                                                            </tr>
                                                                        </table>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                           \s
                                                            <h1 style="color: #ffffff; margin: 20px 0 10px 0; font-size: 28px; font-weight: 800; letter-spacing: -0.5px;">
                                                                HỢP ĐỒNG THUÊ TRỌ
                                                            </h1>
                                                           \s
                                                            <p style="color: rgba(255,255,255,0.9); margin: 0 0 15px 0; font-size: 16px; font-weight: 500;">
                                                                Tài liệu chính thức • Bảo mật cao • Pháp lý đầy đủ
                                                            </p>
                                                           \s
                                                            <!-- Premium Badge -->
                                                            <div style="background: rgba(255,255,255,0.2); padding: 8px 20px; border-radius: 20px; display: inline-block; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px;">
                                                                🛡️ PREMIUM VERIFIED
                                                            </div>
                                                        </td>
                                                    </tr>
                                                   \s
                                                    <!-- 🎯 MAIN CONTENT -->
                                                    <tr>
                                                        <td style="padding: 40px 30px;">
                                                            <!-- Greeting Section -->
                                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                                <tr>
                                                                    <td align="center" style="padding-bottom: 40px;">
                                                                        <div style="background: linear-gradient(135deg, #667eea, #764ba2); color: white; padding: 12px 24px; border-radius: 25px; font-weight: 600; margin-bottom: 20px; display: inline-block;">
                                                                            👤 Kính gửi: %s
                                                                        </div>
                                                                       \s
                                                                        <h2 style="color: #2c3e50; font-size: 24px; margin: 0 0 15px 0; font-weight: 800;">
                                                                            Chào mừng đến với\s
                                                                            <span style="background: linear-gradient(135deg, #667eea, #764ba2); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
                                                                                Nhà Trọ Xanh Premium
                                                                            </span>
                                                                        </h2>
                                                                       \s
                                                                        <p style="color: #7f8c8d; font-size: 16px; line-height: 1.6; margin: 0; max-width: 480px;">
                                                                            Chúng tôi vô cùng hân hạnh được phục vụ bạn. Hợp đồng thuê trọ của bạn đã được chuẩn bị hoàn tất với đầy đủ các điều khoản pháp lý.
                                                                        </p>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                           \s
                                                            <!-- 📋 CONTRACT DETAILS CARD -->
                                                            <div style="background: linear-gradient(135deg, #f8faff 0%%, #e8f4f8 100%%); border-radius: 16px; padding: 30px; margin: 30px 0; border: 1px solid rgba(102, 126, 234, 0.1);">
                                                                <!-- Header -->
                                                                <table width="100%%" cellpadding="0" cellspacing="0">
                                                                    <tr>
                                                                        <td style="padding-bottom: 25px;">
                                                                            <table cellpadding="0" cellspacing="0">
                                                                                <tr>
                                                                                    <td style="width: 50px; height: 50px; background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 12px; text-align: center; vertical-align: middle;">
                                                                                        <div style="font-size: 20px; line-height: 50px;">📋</div>
                                                                                    </td>
                                                                                    <td style="padding-left: 15px; vertical-align: middle;">
                                                                                        <h3 style="color: #2c3e50; margin: 0 0 5px 0; font-size: 20px; font-weight: 800;">
                                                                                            Thông tin hợp đồng
                                                                                        </h3>
                                                                                        <p style="color: #7f8c8d; margin: 0; font-size: 14px;">Chi tiết đầy đủ và chính xác</p>
                                                                                    </td>
                                                                                </tr>
                                                                            </table>
                                                                        </td>
                                                                    </tr>
                                                                </table>
                                                               \s
                                                                <!-- Info Grid Row 1 -->
                                                                <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom: 15px;">
                                                                    <tr>
                                                                        <td width="50%%" style="padding-right: 7px;">
                                                                            <div style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); border-left: 4px solid #667eea;">
                                                                                <div style="color: #667eea; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; opacity: 0.8;">
                                                                                    #️⃣ MÃ HỢP ĐỒNG
                                                                                </div>
                                                                                <div style="color: #2c3e50; font-size: 16px; font-weight: 700;">%s</div>
                                                                            </div>
                                                                        </td>
                                                                        <td width="50%%" style="padding-left: 7px;">
                                                                            <div style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); border-left: 4px solid #28a745;">
                                                                                <div style="color: #28a745; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; opacity: 0.8;">
                                                                                    📅 NGÀY TẠO
                                                                                </div>
                                                                                <div style="color: #2c3e50; font-size: 16px; font-weight: 700;">%s</div>
                                                                            </div>
                                                                        </td>
                                                                    </tr>
                                                                </table>
                                                               \s
                                                                <!-- Info Grid Row 2 -->
                                                                <table width="100%%" cellpadding="0" cellspacing="0">
                                                                    <tr>
                                                                        <td width="50%%" style="padding-right: 7px;">
                                                                            <div style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); border-left: 4px solid #ff6b6b;">
                                                                                <div style="color: #ff6b6b; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; opacity: 0.8;">
                                                                                    📄 ĐỊNH DẠNG
                                                                                </div>
                                                                                <div style="color: #2c3e50; font-size: 16px; font-weight: 700;">PDF Đính kèm</div>
                                                                            </div>
                                                                        </td>
                                                                        <td width="50%%" style="padding-left: 7px;">
                                                                            <div style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); border-left: 4px solid #feca57;">
                                                                                <div style="color: #feca57; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; opacity: 0.8;">
                                                                                    ✅ TRẠNG THÁI
                                                                                </div>
                                                                                <div style="color: #27ae60; font-size: 16px; font-weight: 700;">✅ Hoàn thành</div>
                                                                            </div>
                                                                        </td>
                                                                    </tr>
                                                                </table>
                                                            </div>
                                                           \s
                                                            <!-- 📄 PDF DOWNLOAD SECTION -->
                                                            <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 20px; padding: 40px; text-align: center; color: white; margin: 30px 0;">
                                                                <table width="100%%" cellpadding="0" cellspacing="0">
                                                                    <tr>
                                                                        <td align="center">
                                                                            <!-- PDF Icon -->
                                                                            <table cellpadding="0" cellspacing="0">
                                                                                <tr>
                                                                                    <td style="width: 100px; height: 100px; background: rgba(255,255,255,0.15); border-radius: 20px; text-align: center; vertical-align: middle; border: 2px solid rgba(255,255,255,0.2);">
                                                                                        <div style="font-size: 45px; line-height: 100px;">📄</div>
                                                                                    </td>
                                                                                </tr>
                                                                            </table>
                                                                           \s
                                                                            <h3 style="color: white; margin: 25px 0 15px 0; font-size: 24px; font-weight: 800;">
                                                                                Tài liệu hợp đồng
                                                                            </h3>
                                                                           \s
                                                                            <div style="background: rgba(255,255,255,0.15); padding: 12px 25px; border-radius: 15px; margin: 0 auto 20px; display: inline-block;">
                                                                                <p style="color: white; margin: 0; font-size: 16px; font-weight: 600;">
                                                                                    📝 HopDong_%s.pdf
                                                                                </p>
                                                                            </div>
                                                                           \s
                                                                            <!-- Security Badge -->
                                                                            <div style="background: rgba(255,255,255,0.1); padding: 10px 20px; border-radius: 20px; display: inline-block;">
                                                                                <span style="color: white; font-size: 13px; font-weight: 600;">
                                                                                    🔒 Tệp được mã hóa và bảo mật SSL
                                                                                </span>
                                                                            </div>
                                                                        </td>
                                                                    </tr>
                                                                </table>
                                                            </div>
                                                           \s
                                                            <!-- 📋 INSTRUCTIONS - FIXED VERSION -->
                                                            <div style="background: linear-gradient(135deg, #e8f8f5 0%%, #f0fdf4 100%%); border-left: 5px solid #10b981; padding: 25px; border-radius: 12px; margin: 30px 0;">
                                                                <table width="100%%" cellpadding="0" cellspacing="0">
                                                                    <tr>
                                                                        <td>
                                                                            <!-- Header với icon -->
                                                                            <table cellpadding="0" cellspacing="0" style="margin-bottom: 20px;">
                                                                                <tr>
                                                                                    <td style="width: 45px; height: 45px; background: #10b981; border-radius: 50%%; text-align: center; vertical-align: middle;">
                                                                                        <span style="font-size: 18px; line-height: 1; color: white;">📋</span>
                                                                                    </td>
                                                                                    <td style="padding-left: 15px; vertical-align: middle;">
                                                                                        <h4 style="color: #065f46; margin: 0; font-size: 18px; font-weight: 800;">
                                                                                            Hướng dẫn xử lý hợp đồng
                                                                                        </h4>
                                                                                    </td>
                                                                                </tr>
                                                                            </table>
                                                                           \s
                                                                            <!-- Steps -->
                                                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                                                <!-- Step 1 -->
                                                                                <tr>
                                                                                    <td style="padding-bottom: 12px;">
                                                                                        <table cellpadding="0" cellspacing="0" width="100%%">
                                                                                            <tr>
                                                                                                <td style="width: 25px; height: 25px; background: #10b981; border-radius: 50%%; text-align: center; vertical-align: middle;">
                                                                                                    <span style="color: white; font-size: 12px; font-weight: 700; line-height: 1;">1</span>
                                                                                                </td>
                                                                                                <td style="padding-left: 12px; vertical-align: middle;">
                                                                                                    <span style="color: #065f46; font-size: 15px; font-weight: 600;">
                                                                                                        Tải và mở file PDF đính kèm
                                                                                                    </span>
                                                                                                </td>
                                                                                            </tr>
                                                                                        </table>
                                                                                    </td>
                                                                                </tr>
                                                                               \s
                                                                                <!-- Step 2 -->
                                                                                <tr>
                                                                                    <td style="padding-bottom: 12px;">
                                                                                        <table cellpadding="0" cellspacing="0" width="100%%">
                                                                                            <tr>
                                                                                                <td style="width: 25px; height: 25px; background: #10b981; border-radius: 50%%; text-align: center; vertical-align: middle;">
                                                                                                    <span style="color: white; font-size: 12px; font-weight: 700; line-height: 1;">2</span>
                                                                                                </td>
                                                                                                <td style="padding-left: 12px; vertical-align: middle;">
                                                                                                    <span style="color: #065f46; font-size: 15px; font-weight: 600;">
                                                                                                        Kiểm tra kỹ thông tin cá nhân và điều khoản
                                                                                                    </span>
                                                                                                </td>
                                                                                            </tr>
                                                                                        </table>
                                                                                    </td>
                                                                                </tr>
                                                                               \s
                                                                                <!-- Step 3 -->
                                                                                <tr>
                                                                                    <td style="padding-bottom: 12px;">
                                                                                        <table cellpadding="0" cellspacing="0" width="100%%">
                                                                                            <tr>
                                                                                                <td style="width: 25px; height: 25px; background: #10b981; border-radius: 50%%; text-align: center; vertical-align: middle;">
                                                                                                    <span style="color: white; font-size: 12px; font-weight: 700; line-height: 1;">3</span>
                                                                                                </td>
                                                                                                <td style="padding-left: 12px; vertical-align: middle;">
                                                                                                    <span style="color: #065f46; font-size: 15px; font-weight: 600;">
                                                                                                        In 2 bản hợp đồng và ký tên đầy đủ
                                                                                                    </span>
                                                                                                </td>
                                                                                            </tr>
                                                                                        </table>
                                                                                    </td>
                                                                                </tr>
                                                                               \s
                                                                                <!-- Step 4 -->
                                                                                <tr>
                                                                                    <td>
                                                                                        <table cellpadding="0" cellspacing="0" width="100%%">
                                                                                            <tr>
                                                                                                <td style="width: 25px; height: 25px; background: #10b981; border-radius: 50%%; text-align: center; vertical-align: middle;">
                                                                                                    <span style="color: white; font-size: 12px; font-weight: 700; line-height: 1;">4</span>
                                                                                                </td>
                                                                                                <td style="padding-left: 12px; vertical-align: middle;">
                                                                                                    <span style="color: #065f46; font-size: 15px; font-weight: 600;">
                                                                                                        Liên hệ chúng tôi nếu có bất kỳ thắc mắc nào
                                                                                                    </span>
                                                                                                </td>
                                                                                            </tr>
                                                                                        </table>
                                                                                    </td>
                                                                                </tr>
                                                                            </table>
                                                                        </td>
                                                                    </tr>
                                                                </table>
                                                            </div>
                                                           \s
                                                            <!-- 📞 CONTACT CARDS -->
                                                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                                <tr>
                                                                    <td width="50%%" style="padding-right: 7px;">
                                                                        <div style="background: linear-gradient(135deg, #48dbfb 0%%, #0abde3 100%%); padding: 25px; border-radius: 16px; text-align: center; color: white;">
                                                                            <div style="font-size: 30px; margin-bottom: 12px;">📞</div>
                                                                            <h4 style="margin: 0 0 8px 0; font-size: 16px; font-weight: 700;">Hotline 24/7</h4>
                                                                            <p style="margin: 0; font-size: 18px; font-weight: 800;">1900-xxxx</p>
                                                                        </div>
                                                                    </td>
                                                                    <td width="50%%" style="padding-left: 7px;">
                                                                        <div style="background: linear-gradient(135deg, #ff6b6b 0%%, #ee5a52 100%%); padding: 25px; border-radius: 16px; text-align: center; color: white;">
                                                                            <div style="font-size: 30px; margin-bottom: 12px;">📧</div>
                                                                            <h4 style="margin: 0 0 8px 0; font-size: 16px; font-weight: 700;">Email hỗ trợ</h4>
                                                                            <p style="margin: 0; font-size: 14px; font-weight: 800;">nhatroxanh123@gmail.com</p>
                                                                        </div>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                           \s
                                                            <!-- 🌐 WEBSITE INFO -->
                                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                                <tr>
                                                                    <td align="center" style="padding: 30px 0;">
                                                                        <div style="background: linear-gradient(135deg, #ffeaa7 0%%, #fdcb6e 100%%); padding: 20px 30px; border-radius: 16px; display: inline-block; color: #2d3436;">
                                                                            <div style="font-size: 28px; margin-bottom: 10px;">🌐</div>
                                                                            <p style="margin: 0; font-weight: 800; font-size: 16px;">www.nhatroxanh.com</p>
                                                                        </div>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                        </td>
                                                    </tr>
                                                   \s
                                                    <!-- 🏢 PREMIUM FOOTER -->
                                                    <tr>
                                                        <td style="background: linear-gradient(135deg, #2c3e50 0%%, #34495e 100%%); padding: 35px 30px; text-align: center; color: white;">
                                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                                <tr>
                                                                    <td align="center">
                                                                        <table cellpadding="0" cellspacing="0">
                                                                            <tr>
                                                                                <td style="width: 60px; height: 60px; background: rgba(255,255,255,0.1); border-radius: 50%%; text-align: center; vertical-align: middle; border: 2px solid rgba(255,255,255,0.2);">
                                                                                    <div style="font-size: 24px; line-height: 60px;">🏢</div>
                                                                                </td>
                                                                            </tr>
                                                                        </table>
                                                                       \s
                                                                        <h3 style="margin: 15px 0 10px 0; font-size: 20px; font-weight: 800;">
                                                                            NHÀ TRỌ XANH PREMIUM
                                                                        </h3>
                                                                        <p style="margin: 0 0 20px 0; font-size: 14px; opacity: 0.9;">
                                                                            Dịch vụ cho thuê nhà trọ chuyên nghiệp • Uy tín • Chất lượng cao
                                                                        </p>
                                                                       \s
                                                                        <div style="border-top: 1px solid rgba(255,255,255,0.15); padding-top: 20px;">
                                                                            <p style="margin: 0 0 10px 0; font-size: 15px; font-weight: 700;">
                                                                                ❤️ Cảm ơn bạn đã tin tưởng dịch vụ của chúng tôi!
                                                                            </p>
                                                                            <p style="margin: 0; font-size: 12px; opacity: 0.8; line-height: 1.5;">
                                                                                © 2024 Nhà Trọ Xanh Premium. Tất cả quyền được bảo lưu.<br>
                                                                                Email này được gửi tự động, vui lòng không trả lời trực tiếp.
                                                                            </p>
                                                                        </div>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </body>
                                </html>

                        """,
                customerName, contractNumber, currentDate, contractNumber.replace(" ", "_"));
    }

    // ✅ HÀM GỬI EMAIL HTML (GIỮ NGUYÊN)
    private void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("nhatroxanh123@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    @Override
    public void sendTextEmail(String to, String subject, String text) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, false); // false = text/plain

        mailSender.send(message);
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = text/html

        mailSender.send(message);
    }

    @Override
    public void sendEmail(String to, String subject, String body) throws Exception {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("nhatroxanh123@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (Exception e) {
            throw new Exception("Không thể gửi email: " + e.getMessage());
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String body,
            byte[] attachmentData, String fileName) throws Exception {
        try {
            // ✅ VALIDATE ATTACHMENT
            if (attachmentData == null || attachmentData.length == 0) {
                throw new Exception("Attachment data is null or empty!");
            }

            // ✅ VALIDATE PDF IF IT'S PDF
            if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                if (attachmentData.length > 4) {
                    String header = new String(attachmentData, 0, Math.min(10, attachmentData.length));

                    if (!header.startsWith("%PDF")) {
                        throw new Exception("Invalid PDF format!");
                    }
                }
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("nhatroxanh123@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true for HTML

            // ✅ CREATE DATASOURCE
            ByteArrayDataSource dataSource = new ByteArrayDataSource(attachmentData, "application/pdf");
            dataSource.setName(fileName);
            // ✅ ADD ATTACHMENT
            helper.addAttachment(fileName, dataSource);
            // ✅ SEND
            mailSender.send(message);

        } catch (Exception e) {
            throw new Exception("Không thể gửi email với attachment: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendContractPDF(String to, String tenantName, String roomName,
            byte[] pdfData, String fileName) throws Exception {
        try {
            String subject = "📋 Hợp đồng thuê trọ - Phòng " + roomName;

            // ✅ SỬA TÊN FILE CHO ĐÚNG
            String cleanFileName = "HopDong_" + roomName.replaceAll("\\s+", "_") + "_" +
                    tenantName.replaceAll("\\s+", "_") + ".pdf";

            String htmlBody = buildContractEmailBody(tenantName, roomName);

            // ✅ DÙNG TÊN FILE ĐÃ SỬA
            sendEmailWithAttachment(to, subject, htmlBody, pdfData, cleanFileName);
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi PDF hợp đồng: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void sendVoucherDeactivationEmail(Vouchers voucher) {
        Users creator = voucher.getUser();

        if (creator == null || creator.getEmail() == null) {
            throw new IllegalArgumentException("Voucher không có người tạo hoặc email không hợp lệ.");
        }

        String title = "Voucher hết hạn/hết số lượng";
        String greeting = "Xin chào " + creator.getFullname();
        String content = String.format("""
                <p>Voucher bạn tạo đã bị vô hiệu hóa do hết hạn hoặc hết số lượng sử dụng.</p>
                <div class="highlight">
                    <p><strong>Tên voucher:</strong> %s</p>
                    <p><strong>Mã voucher:</strong> %s</p>
                    <p><strong>Thời gian áp dụng:</strong> %s → %s</p>
                    <p><strong>Số lượng còn lại:</strong> %d</p>
                </div>
                <p>Vui lòng tạo voucher mới nếu bạn muốn tiếp tục chương trình khuyến mãi.</p>
                """,
                voucher.getTitle(), voucher.getCode(),
                voucher.getStartDate().toString(), voucher.getEndDate().toString(),
                voucher.getQuantity());

        sendHtmlMail(creator.getEmail(), title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendPostApprovedEmail(String to, String fullname, String postTitle) {
        String title = "Bài đăng được phê duyệt";
        String greeting = "Xin chào " + fullname;
        String content = String.format("""
                <p>Bài đăng của bạn đã được phê duyệt và hiện đang hiển thị trên hệ thống.</p>
                <div class="highlight">
                    <p><strong>Tiêu đề bài đăng:</strong> %s</p>
                </div>
                <p>Cảm ơn bạn đã đăng bài trên hệ thống của chúng tôi.</p>
                """, postTitle);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendPostRejectedEmail(String to, String fullname, String postTitle) {
        String title = "Bài đăng bị từ chối";
        String greeting = "Xin chào " + fullname;
        String content = String.format("""
                <p>Bài đăng của bạn đã không được phê duyệt.</p>
                <div class="highlight">
                    <p><strong>Tiêu đề bài đăng:</strong> %s</p>
                </div>
                <p>Vui lòng kiểm tra lại nội dung và gửi lại bài đăng.</p>
                """, postTitle);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendOwnerApprovalEmail(String to, String fullname) {
        String title = "Đăng ký chủ trọ thành công";
        String greeting = "Xin chào " + fullname;
        String content = """
                <p>Chúc mừng! Yêu cầu đăng ký làm chủ trọ của bạn đã được phê duyệt.</p>
                <div class="highlight">
                    <p>Tài khoản của bạn đã được nâng cấp lên quyền <strong>Chủ trọ</strong>.</p>
                </div>
                <p>Bạn có thể bắt đầu sử dụng các tính năng quản lý phòng trọ ngay bây giờ.</p>
                """;

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendOwnerRejectionEmail(String to, String fullname) {
        String title = "Đăng ký chủ trọ không thành công";
        String greeting = "Xin chào " + fullname;
        String content = """
                <p>Rất tiếc! Yêu cầu đăng ký làm chủ trọ của bạn đã không được chấp thuận.</p>
                <div class="highlight">
                    <p>Nếu bạn cần hỗ trợ thêm về yêu cầu này, vui lòng liên hệ với quản trị viên hệ thống.</p>
                </div>
                <p>Xin cảm ơn.</p>
                """;

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("nhatroxanh123@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    public void sendNewPasswordEmail(String to, String fullname, String newPassword) {
        String title = "Mật khẩu mới của bạn";
        String greeting = "Xin chào " + fullname;
        String content = String.format("""
                <p>Bạn đã yêu cầu cấp lại mật khẩu. Dưới đây là mật khẩu mới của bạn:</p>
                <div class="highlight">
                    <p><strong>Mật khẩu mới:</strong> %s</p>
                </div>
                <p>Vui lòng đăng nhập và thay đổi mật khẩu này ngay lập tức để bảo mật tài khoản.</p>
                """, newPassword);

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendCashPaymentAppointmentEmail(String to, String landlordName, String tenantName,
            String roomName, String hostelName, String paymentDate,
            String paymentTime, String amount, String note, Integer paymentId) {
        String title = "Lịch hẹn thanh toán tiền mặt - Hóa đơn #" + paymentId;

        String content = String.format(
                "<div style='background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                        "<h3 style='color: #28a745; margin-bottom: 15px;'>📅 Thông tin lịch hẹn thanh toán</h3>" +
                        "<table style='width: 100%%; border-collapse: collapse;'>" +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Người thuê:</td><td style='padding: 8px 0;'>%s</td></tr>"
                        +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Phòng:</td><td style='padding: 8px 0;'>%s</td></tr>"
                        +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Nhà trọ:</td><td style='padding: 8px 0;'>%s</td></tr>"
                        +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Ngày hẹn:</td><td style='padding: 8px 0; color: #dc3545;'>%s</td></tr>"
                        +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Giờ hẹn:</td><td style='padding: 8px 0; color: #dc3545;'>%s</td></tr>"
                        +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Số tiền:</td><td style='padding: 8px 0; color: #28a745; font-size: 18px; font-weight: bold;'>%s</td></tr>"
                        +
                        "%s" +
                        "</table>" +
                        "</div>" +
                        "<div style='background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 15px 0;'>"
                        +
                        "<p style='margin: 0; color: #856404;'><strong>⚠️ Lưu ý:</strong> Vui lòng xác nhận việc nhận thanh toán trong hệ thống sau khi người thuê đã thanh toán.</p>"
                        +
                        "</div>",
                tenantName, roomName, hostelName, paymentDate, paymentTime, amount,
                (note != null && !note.trim().isEmpty()) ? String.format(
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Ghi chú:</td><td style='padding: 8px 0;'>%s</td></tr>",
                        note) : "");

        String greeting = String.format("Xin chào %s", landlordName);
        String footer = "Vui lòng đăng nhập vào hệ thống để xác nhận thanh toán sau khi nhận tiền từ người thuê.";

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, footer));
    }

    @Override
    public void sendCashPaymentSuccessEmail(String to, String tenantName, String roomName,
            String hostelName, String amount, String paymentDate, Integer paymentId) {
        String title = "Thanh toán thành công - Hóa đơn #" + paymentId;

        String content = String.format(
                "<div style='background: #d4edda; border: 1px solid #c3e6cb; padding: 20px; border-radius: 8px; margin: 20px 0;'>"
                        +
                        "<h3 style='color: #155724; margin-bottom: 15px;'>✅ Thanh toán đã được xác nhận</h3>" +
                        "<table style='width: 100%%; border-collapse: collapse;'>" +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Phòng:</td><td style='padding: 8px 0;'>%s</td></tr>"
                        +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Nhà trọ:</td><td style='padding: 8px 0;'>%s</td></tr>"
                        +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Số tiền đã thanh toán:</td><td style='padding: 8px 0; color: #28a745; font-size: 18px; font-weight: bold;'>%s</td></tr>"
                        +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Ngày thanh toán:</td><td style='padding: 8px 0;'>%s</td></tr>"
                        +
                        "<tr><td style='padding: 8px 0; font-weight: bold;'>Phương thức:</td><td style='padding: 8px 0;'>Tiền mặt</td></tr>"
                        +
                        "</table>" +
                        "</div>" +
                        "<div style='background: #d1ecf1; border: 1px solid #bee5eb; padding: 15px; border-radius: 5px; margin: 15px 0;'>"
                        +
                        "<p style='margin: 0; color: #0c5460;'><strong>💡 Thông tin:</strong> Chủ trọ đã xác nhận đã nhận được thanh toán của bạn. Cảm ơn bạn đã thanh toán đúng hạn!</p>"
                        +
                        "</div>",
                roomName, hostelName, amount, paymentDate);

        String greeting = String.format("Xin chào %s", tenantName);
        String footer = "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!";

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, footer));
    }

    @Override
    public void sendPrivacyPolicyEmail(String to) {
        String title = "Chính sách bảo mật & Điều khoản sử dụng";

        String content = "<div style='background: #f8f9fa; border: 1px solid #e0e0e0; padding: 20px; border-radius: 8px; margin: 20px 0;'>"
                +
                "<h3 style='color: #1a73e8; margin-top: 0;'>CHÍNH SÁCH BẢO MẬT & ĐIỀU KHOẢN SỬ DỤNG</h3>" +
                "<p style='font-size: 14px; color: #555;'>(Áp dụng cho hệ thống Quản lý Nhà trọ Xanh)</p>" +

                "<h4 style='color: #1a73e8;'>1. Mục đích</h4>" +
                "<p>Chính sách này nhằm bảo vệ thông tin cá nhân của người dùng và quy định cách thức sử dụng dịch vụ quản lý nhà trọ của chúng tôi, đảm bảo quyền lợi hợp pháp của tất cả các bên tham gia.</p>"
                +

                "<h4 style='color: #1a73e8;'>2. Thu thập thông tin</h4>" +
                "<ul style='margin-top: 0; padding-left: 18px;'>" +
                "<li>Họ tên, số điện thoại, email, ngày sinh.</li>" +
                "<li>Thông tin tài khoản ngân hàng (nếu có giao dịch tài chính).</li>" +
                "<li>Địa chỉ nhà trọ, thông tin phòng, hợp đồng thuê.</li>" +
                "<li>Lịch sử giao dịch, thanh toán và trao đổi với chủ nhà/khách thuê.</li>" +
                "</ul>" +

                "<h4 style='color: #1a73e8;'>3. Mục đích sử dụng thông tin</h4>" +
                "<ul style='padding-left: 18px;'>" +
                "<li>Quản lý nhà trọ, phòng cho thuê và hợp đồng.</li>" +
                "<li>Liên hệ, gửi thông báo, hóa đơn, hoặc thông tin liên quan.</li>" +
                "<li>Hỗ trợ kỹ thuật và xử lý khiếu nại.</li>" +
                "<li>Cải thiện chất lượng dịch vụ và trải nghiệm người dùng.</li>" +
                "<li>Đáp ứng yêu cầu của cơ quan quản lý nhà nước khi cần thiết.</li>" +
                "</ul>" +

                "<h4 style='color: #1a73e8;'>4. Bảo mật thông tin</h4>" +
                "<p>Chúng tôi cam kết không bán, trao đổi hoặc chia sẻ thông tin cá nhân của bạn cho bên thứ ba, trừ khi có sự đồng ý của bạn hoặc yêu cầu từ cơ quan pháp luật.</p>"
                +
                "<p>Thông tin được lưu trữ trên hệ thống bảo mật cao, có cơ chế mã hóa dữ liệu và sao lưu định kỳ.</p>"
                +
                "<p>Nhân viên chỉ được truy cập thông tin trong phạm vi cần thiết để thực hiện công việc.</p>" +

                "<h4 style='color: #1a73e8;'>5. Quyền của người dùng</h4>" +
                "<ul style='padding-left: 18px;'>" +
                "<li>Yêu cầu xem, chỉnh sửa hoặc xóa thông tin cá nhân của mình.</li>" +
                "<li>Yêu cầu ngừng nhận thông báo hoặc email quảng bá.</li>" +
                "<li>Gửi khiếu nại về việc sử dụng thông tin cá nhân.</li>" +
                "</ul>" +

                "<h4 style='color: #1a73e8;'>6. Trách nhiệm của người dùng</h4>" +
                "<ul style='padding-left: 18px;'>" +
                "<li>Cung cấp thông tin chính xác và cập nhật kịp thời.</li>" +
                "<li>Không sử dụng hệ thống vào mục đích trái pháp luật.</li>" +
                "<li>Bảo mật tài khoản và không chia sẻ thông tin đăng nhập cho người khác.</li>" +
                "</ul>" +

                "<h4 style='color: #1a73e8;'>7. Thay đổi chính sách</h4>" +
                "<p>Chúng tôi có thể cập nhật chính sách này theo từng thời điểm để phù hợp với yêu cầu pháp luật và nhu cầu dịch vụ. Mọi thay đổi sẽ được thông báo trên website và/hoặc qua email.</p>"
                +

                "<h4 style='color: #1a73e8;'>8. Liên hệ</h4>" +
                "<p>📧 Email: <a href='mailto:nhatroxanh123@gmail.com' style='color: #1a73e8; text-decoration: none;'>nhatroxanh123@gmail.com</a><br>"
                +
                "📞 Hotline: <a href='tel:09888999999' style='color: #1a73e8; text-decoration: none;'>0988 999 999</a></p>"
                +
                "</div>";

        String greeting = "Xin chào Quý khách";
        String footer = "Cảm ơn bạn đã sử dụng dịch vụ của Nhà Trọ Xanh!";

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, footer));
    }

    @Override
    public void sendExtensionRequestEmail(String to, String landlordName, String tenantName, String contractCode,
            Date requestedExtendDate, String message) {
        String title = "Yêu cầu gia hạn hợp đồng mới";
        String greeting = "Xin chào " + landlordName;
        String content = String.format("""
                <p>Khách thuê <strong>%s</strong> đã gửi yêu cầu gia hạn hợp đồng.</p>
                <div class="highlight">
                    <p><strong>Mã hợp đồng:</strong> %s</p>
                    <p><strong>Ngày gia hạn đề xuất:</strong> %s</p>
                    %s
                </div>
                <p>Vui lòng đăng nhập vào hệ thống để xem xét và phản hồi yêu cầu này.</p>
                <a href="http://www.nhatroxanh.com/login" class="button">Xem yêu cầu</a>
                """, tenantName, contractCode, new SimpleDateFormat("dd/MM/yyyy").format(requestedExtendDate),
                (message != null && !message.trim().isEmpty())
                        ? String.format("<p><strong>Ghi chú:</strong> %s</p>", message)
                        : "");

        String footer = "Vui lòng xử lý yêu cầu trong vòng 48 giờ để đảm bảo trải nghiệm tốt nhất cho khách thuê.";
        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, footer));
    }

    @Override
    public void sendReturnRequestEmail(String to, String landlordName, String tenantName, String contractCode,
            Date requestedReturnDate, String returnReason) {
        String title = "Yêu cầu trả phòng mới";
        String greeting = "Xin chào " + landlordName;
        String content = String.format("""
                <p>Khách thuê <strong>%s</strong> đã gửi yêu cầu trả phòng.</p>
                <div class="highlight">
                    <p><strong>Mã hợp đồng:</strong> %s</p>
                    <p><strong>Ngày trả phòng đề xuất:</strong> %s</p>
                    %s
                </div>
                <p>Vui lòng đăng nhập vào hệ thống để xem xét và phản hồi yêu cầu này.</p>
                <a href="http://www.nhatroxanh.com/login" class="button">Xem yêu cầu</a>
                """, tenantName, contractCode, new SimpleDateFormat("dd/MM/yyyy").format(requestedReturnDate),
                (returnReason != null && !returnReason.trim().isEmpty())
                        ? String.format("<p><strong>Lý do trả phòng:</strong> %s</p>", returnReason)
                        : "");

        String footer = "Vui lòng xử lý yêu cầu trong vòng 48 giờ để đảm bảo trải nghiệm tốt nhất cho khách thuê.";
        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, footer));
    }

    @Override
    public void sendTransactionApprovedEmail(String to, String fullname, String transactionReference,
            String transactionType, Double amount, String processedAt, String approvalNote) {
        String title = "Giao Dịch Đã Được Duyệt";
        String greeting = "Kính gửi " + fullname;
        String content = "<div style=\"background-color: #f5f5f5; padding: 20px; font-family: Arial, sans-serif;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);\">"
                +
                "<div style=\"background: linear-gradient(to right, #4CAF50, #2E7D32); padding: 20px; text-align: center;\">"
                +
                "<h1 style=\"color: white; margin: 0; padding: 10px 0;\">Giao Dịch Đã Được Duyệt</h1>" + // Thêm padding
                "</div>" +
                "<div style=\"padding: 30px; text-align: center;\">" +
                "<p style=\"font-size: 16px; line-height: 1.6; color: #333; margin-bottom: 25px;\">" + // Tăng
                                                                                                       // margin-bottom
                "Giao dịch của bạn đã được duyệt thành công.<br> Dưới đây là thông tin chi tiết:" +
                "</p>" +
                "<div style=\"background-color: #f9f9f9; border-left: 4px solid #4CAF50; padding: 20px; margin: 0 auto 25px auto; max-width: 500px; border-radius: 4px;\">"
                + // Tăng padding và margin
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee;\">"
                + // Tăng margin và padding
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Mã giao dịch:</span>"
                + // Thêm min-width
                "<span style=\"color: #333; text-align: right;\">" + transactionReference + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee;\">"
                +
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Loại giao dịch:</span>"
                +
                "<span style=\"color: #333; text-align: right;\">" + transactionType + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee;\">"
                +
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Số tiền:</span>" +
                "<span style=\"color: #333; font-weight: bold; text-align: right;\">" + String.format("%,.0f", amount)
                + " VNĐ</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee;\">"
                +
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Thời gian xử lý:</span>"
                +
                "<span style=\"color: #333; text-align: right;\">" + processedAt + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between;\">" +
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Ghi chú:</span>" +
                "<span style=\"color: #333; text-align: right;\">" + (approvalNote != null ? approvalNote : "Không có")
                + "</span>" +
                "</div>" +
                "</div>" +
                "<p style=\"font-size: 16px; line-height: 1.6; color: #333; margin-top: 20px;\">" + // Thêm margin-top
                "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi." +
                "</p>" +
                "</div>" +
                "<div style=\"background-color: #f5f5f5; padding: 20px; text-align: center; color: #777; font-size: 14px;\">"
                +
                "</div>" +
                "</div>" +
                "</div>";

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }

    @Override
    public void sendTransactionRejectedEmail(String to, String fullname, String transactionReference,
            String transactionType, Double amount, String processedAt, String rejectionReason) {
        String title = "Giao Dịch Bị Từ Chối";
        String greeting = "Kính gửi " + fullname;
        String content = "<div style=\"background-color: #f5f5f5; padding: 20px; font-family: Arial, sans-serif;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);\">"
                +
                "<div style=\"background: linear-gradient(to right, #F44336, #D32F2F); padding: 20px; text-align: center;\">"
                +
                "<h1 style=\"color: white; margin: 0; padding: 10px 0;\">Giao Dịch Bị Từ Chối</h1>" + // Thêm padding
                "</div>" +
                "<div style=\"padding: 30px; text-align: center;\">" +
                "<p style=\"font-size: 16px; line-height: 1.6; color: #333; margin-bottom: 25px;\">" + // Tăng
                                                                                                       // margin-bottom
                "Rất tiếc, giao dịch của bạn đã bị từ chối. Dưới đây là thông tin chi tiết:" +
                "</p>" +
                "<div style=\"background-color: #f9f9f9; border-left: 4px solid #F44336; padding: 20px; margin: 0 auto 25px auto; max-width: 500px; border-radius: 4px;\">"
                + // Tăng padding và margin
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee;\">"
                + // Tăng margin và padding
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Mã giao dịch:</span>"
                + // Thêm min-width
                "<span style=\"color: #333; text-align: right;\">" + transactionReference + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee;\">"
                +
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Loại giao dịch:</span>"
                +
                "<span style=\"color: #333; text-align: right;\">" + transactionType + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee;\">"
                +
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Số tiền:</span>" +
                "<span style=\"color: #333; font-weight: bold; text-align: right;\">" + String.format("%,.0f", amount)
                + " VNĐ</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee;\">"
                +
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Thời gian xử lý:</span>"
                +
                "<span style=\"color: #333; text-align: right;\">" + processedAt + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between;\">" +
                "<span style=\"font-weight: bold; color: #555; min-width: 120px; text-align: left;\">Lý do từ chối:</span>"
                +
                "<span style=\"color: #333; text-align: right;\">" + rejectionReason + "</span>" +
                "</div>" +
                "</div>" +
                "<p style=\"font-size: 16px; line-height: 1.6; color: #333; margin-top: 20px;\">" + // Thêm margin-top
                "Vui lòng liên hệ hỗ trợ nếu cần thêm thông tin." +
                "</p>" +
                "</div>" +
                "<div style=\"background-color: #f5f5f5; padding: 20px; text-align: center; color: #777; font-size: 14px;\">"
                +
                "</div>" +
                "</div>" +
                "</div>";

        sendHtmlMail(to, title, getEmailTemplate(title, content, greeting, ""));
    }
}
