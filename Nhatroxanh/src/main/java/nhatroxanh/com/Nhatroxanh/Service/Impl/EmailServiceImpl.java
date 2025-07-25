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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendExtensionApprovalEmail(String to, String fullname, String contractCode, Date newEndDate) {
        String subject = "Yêu cầu gia hạn đã được phê duyệt";
        String content = "<p>Chào " + fullname + ",</p>" +
                "<p>Yêu cầu gia hạn hợp đồng <b>" + contractCode + "</b> của bạn đã được phê duyệt.</p>" +
                "<p>Ngày kết thúc mới: <b>" + newEndDate + "</b></p>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ Nhà Trọ Xanh.</p>";

        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendExtensionRejectionEmail(String to, String fullname, String contractCode, String reason) {
        String subject = "Yêu cầu gia hạn bị từ chối";
        String content = "<p>Chào " + fullname + ",</p>" +
                "<p>Rất tiếc! Yêu cầu gia hạn hợp đồng <b>" + contractCode + "</b> đã bị từ chối.</p>" +
                "<p>Lý do: <i>" + reason + "</i></p>" +
                "<p>Nếu bạn có thắc mắc, vui lòng liên hệ quản lý.</p>";

        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendExpirationWarningEmail(String to, String fullname, String contractCode, Date endDate) {
        String subject = "Cảnh báo: Hợp đồng thuê sắp hết hạn";
        String content = "<p>Chào " + fullname + ",</p>" +
                "<p>Hợp đồng <b>" + contractCode + "</b> của bạn sẽ hết hạn vào ngày <b>" + endDate + "</b>.</p>" +
                "<p>Vui lòng gia hạn hợp đồng hoặc liên hệ quản lý để biết thêm chi tiết.</p>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ Nhà Trọ Xanh.</p>";

        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendReturnApprovalEmail(String to, String fullname, String contractCode, Date endDate) {
        String subject = "Yêu cầu trả phòng đã được duyệt";
        String content = "<p>Chào " + fullname + ",</p>" +
                "<p>Yêu cầu trả phòng của bạn cho hợp đồng <b>" + contractCode + "</b> đã được <b>phê duyệt</b>.</p>" +
                "<p>Ngày kết thúc hợp đồng: <b>" + endDate + "</b></p>" +
                "<p>Chúng tôi hy vọng bạn hài lòng với dịch vụ của Nhà Trọ Xanh.</p>";

        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendReturnRejectionEmail(String to, String fullname, String contractCode, String reason) {
        String subject = "Yêu cầu trả phòng bị từ chối";
        String content = "<p>Chào " + fullname + ",</p>" +
                "<p>Rất tiếc! Yêu cầu trả phòng cho hợp đồng <b>" + contractCode + "</b> đã bị từ chối.</p>" +
                "<p>Lý do: <i>" + reason + "</i></p>" +
                "<p>Nếu bạn cần hỗ trợ thêm, vui lòng liên hệ với quản lý khu trọ.</p>";

        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendContractTerminatedEmail(String to, String fullname, String contractCode, Date endDate) {
        String subject = "Hợp đồng thuê đã kết thúc";
        String content = "<p>Chào " + fullname + ",</p>" +
                "<p>Hợp đồng <b>" + contractCode + "</b> của bạn đã kết thúc vào ngày <b>" + endDate + "</b>.</p>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ Nhà Trọ Xanh. Nếu bạn cần hỗ trợ thêm, vui lòng liên hệ chúng tôi.</p>";

        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendVoucherDeactivatedEmail(String to, String fullname, String voucherTitle, String reason) {
        String subject = "Voucher đã ngừng hoạt động";
        String content = "<p>Chào " + fullname + ",</p>" +
                "<p>Voucher <strong>" + voucherTitle +
                "</strong> của bạn đã được chuyển sang trạng thái <b>ngừng hoạt động</b>.</p>" +
                "<p>Lý do: <i>" + reason + "</i></p>" +
                "<p>Vui lòng kiểm tra lại hệ thống nếu cần kích hoạt lại hoặc tạo voucher mới.</p>" +
                "<p>Trân trọng,<br>Nhà Trọ Xanh</p>";

        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendIncidentProcessingEmail(String to, IncidentReports incident) {
        String subject = "Sự cố đang được xử lý";
        String content = String.format(
                "Xin chào,\n\nSự cố \"%s\" của bạn đã được tiếp nhận và đang được xử lý.\nThời gian xử lý: %s\n\nTrân trọng.",
                incident.getIncidentType(),
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(incident.getResolvedAt()));
        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendIncidentResolvedEmail(String to, IncidentReports incident) {
        String subject = "Sự cố đã được xử lý";
        String content = String.format(
                "Xin chào,\n\nSự cố \"%s\" của bạn đã được xử lý thành công.\n\nTrân trọng.",
                incident.getIncidentType());
        sendHtmlMail(to, subject, content);
    }

    @Override

    public void sendContractEmail(String to, String customerName, byte[] pdfContent, String contractNumber) throws Exception {
        logger.info("🔧 MANUAL MULTIPART APPROACH");

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

            logger.info("✅ Manual multipart email sent successfully!");

        } catch (Exception e) {
            logger.error("❌ Manual multipart error: {}", e.getMessage(), e);
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
                .replaceAll("_{2,}", "_")  // Replace multiple underscores with single
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
    private String buildContractEmailBody(String customerName, String contractNumber) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        return String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h1 style="margin: 0; font-size: 24px;">🏠 Nhà Trọ Xanh</h1>
                    <p style="margin: 5px 0 0 0;">Hợp đồng thuê nhà điện tử</p>
                </div>
           
                <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #333;">Kính chào <span style="color: #28a745; font-weight: bold;">%s</span>,</h2>
                    
                    <p>Chúng tôi gửi đến bạn <strong>Hợp đồng thuê nhà số %s</strong> đã được tạo vào lúc %s.</p>
                    
                    <div style="background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745;">
                        <h3 style="color: #333; margin-top: 0;">📄 Thông tin hợp đồng:</h3>
                        <ul style="list-style: none; padding: 0;">
                            <li style="margin: 8px 0;"><strong>Số hợp đồng:</strong> %s</li>
                            <li style="margin: 8px 0;"><strong>Ngày tạo:</strong> %s</li>
                            <li style="margin: 8px 0;"><strong>Định dạng:</strong> PDF đính kèm</li>
                            <li style="margin: 8px 0;"><strong>Trạng thái:</strong> <span style="color: #28a745;">✅ Đã hoàn thành</span></li>
                        </ul>
                    </div>
                    
                    <div style="background: #e3f2fd; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="color: #333; margin-top: 0;">📋 Hướng dẫn:</h4>
                        <ol>
                            <li>Tải file PDF đính kèm</li>
                            <li>Kiểm tra thông tin trong hợp đồng</li>
                            <li>In 2 bản và ký tên</li>
                            <li>Liên hệ chúng tôi nếu có thắc mắc</li>
                        </ol>
                    </div>
                    
                    <p>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ:</p>
                    <ul style="list-style: none; padding: 0;">
                        <li style="margin: 5px 0;">📞 <strong>Hotline:</strong> 1900-xxxx</li>
                        <li style="margin: 5px 0;">📧 <strong>Email:</strong> nhatroxanh123@gmail.com</li>
                        <li style="margin: 5px 0;">🌐 <strong>Website:</strong> www.nhatroxanh.com</li>
                    </ul>
                    
                    <p>Cảm ơn bạn đã tin tưởng dịch vụ của <strong>Nhà Trọ Xanh</strong>!</p>
                </div>
                
                <div style="text-align: center; margin-top: 20px; font-size: 12px; color: #6c757d;">
                    <p>© 2024 Nhà Trọ Xanh. Tất cả quyền được bảo lưu.</p>
                    <p>Email này được gửi tự động, vui lòng không reply.</p>
                </div>
            </div>
            """,
                customerName, contractNumber, currentDate,
                contractNumber, currentDate
        );
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
    public void sendEmail(String to, String subject, String body) throws Exception {
        logger.info("📧 === BẮT ĐẦU GỬI EMAIL ===");
        logger.info("📧 To: {}", to);
        logger.info("📧 Subject: {}", subject);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("nhatroxanh123@gmail.com"); // ✅ Thay bằng email của bạn
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            logger.info("✅ Email đã được gửi thành công!");

        } catch (Exception e) {
            logger.error("❌ Lỗi khi gửi email: {}", e.getMessage(), e);
            throw new Exception("Không thể gửi email: " + e.getMessage());
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String body,
                                        byte[] attachmentData, String fileName) throws Exception {
        logger.info("🔧 DEBUG: sendEmailWithAttachment called - FIXED VERSION");
        logger.info("📧 To: {}", to);
        logger.info("📎 Filename: {}", fileName);
        logger.info("📊 Data size: {} bytes", attachmentData != null ? attachmentData.length : 0);

        try {
            // ✅ VALIDATE ATTACHMENT
            if (attachmentData == null || attachmentData.length == 0) {
                throw new Exception("Attachment data is null or empty!");
            }

            // ✅ VALIDATE PDF IF IT'S PDF
            if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                if (attachmentData.length > 4) {
                    String header = new String(attachmentData, 0, Math.min(10, attachmentData.length));
                    logger.info("🔧 DEBUG: PDF header check: [{}]", header);

                    if (!header.startsWith("%PDF")) {
                        logger.error("❌ Invalid PDF header: {}", header);
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

            logger.info("🔧 DEBUG: DataSource created - name: {}, type: {}",
                    dataSource.getName(), dataSource.getContentType());

            // ✅ ADD ATTACHMENT
            helper.addAttachment(fileName, dataSource);

            logger.info("🔧 DEBUG: Attachment added successfully");

            // ✅ SEND
            mailSender.send(message);

            logger.info("✅ Email with attachment sent successfully to: {}", to);
            logger.info("📎 Attachment: {} ({} bytes)", fileName, attachmentData.length);

        } catch (Exception e) {
            logger.error("❌ Error in sendEmailWithAttachment: {}", e.getMessage(), e);
            throw new Exception("Không thể gửi email với attachment: " + e.getMessage(), e);
        }
    }



    @Override
    public void sendContractPDF(String to, String tenantName, String roomName,
                                byte[] pdfData, String fileName) throws Exception {
        try {
            String subject = "📋 Hợp đồng thuê trọ - Phòng " + roomName;
            String body = "Xin chào " + tenantName + ",\n\n" +
                    "Đính kèm là file PDF hợp đồng thuê trọ phòng " + roomName + ".\n\n" +
                    "Vui lòng kiểm tra kỹ thông tin và liên hệ nếu có thắc mắc.\n\n" +
                    "Cảm ơn bạn đã tin tưởng dịch vụ của chúng tôi!\n\n" +
                    "Trân trọng!\n" +
                    "Ban quản lý";

            sendEmailWithAttachment(to, subject, body, pdfData, fileName);
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi PDF hợp đồng: " + e.getMessage());
            throw e;
        }
    }

    // ✅ METHOD MỚI GỬI HTML
    @Override
    public void sendContractHtml(String recipientEmail, String recipientName, String subject, String contractHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject(subject != null ? subject : "Hợp đồng thuê nhà");
            helper.setFrom("nhatroxanh@gmail.com", "Nhà Trọ Xanh");

            // ✅ SET HTML CONTENT THAY VÌ TEXT
            helper.setText(contractHtml, true); // true = HTML content

            mailSender.send(message);
            System.out.println("✅ Email HTML sent successfully to: " + recipientEmail);

        } catch (Exception e) {
            System.err.println("❌ Error sending HTML email: " + e.getMessage());
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    public void sendVoucherDeactivationEmail(Vouchers voucher) {
        Users creator = voucher.getUser();

        if (creator == null || creator.getEmail() == null) {
            throw new IllegalArgumentException("Voucher không có người tạo hoặc email không hợp lệ.");
        }

        String to = creator.getEmail();
        String subject = "Voucher " + voucher.getCode() + " đã hết hạn hoặc hết số lượng";
        String content = String.format("""
                Xin chào %s,

                Voucher bạn tạo với mã [%s] đã bị vô hiệu hóa vì đã hết hạn hoặc hết số lượng sử dụng.

                Thông tin:
                - Tên khuyến mãi: %s
                - Mã voucher: %s
                - Thời gian áp dụng: %s → %s
                - Số lượng còn lại: %d
                - Trạng thái mới: Ngừng hoạt động

                Vui lòng kiểm tra lại hệ thống nếu cần gia hạn hoặc tạo mới.

                Trân trọng,
                Hệ thống Nhà trọ Xanh
                """,
                creator.getFullname(),
                voucher.getCode(),
                voucher.getTitle(),
                voucher.getStartDate().toString(),
                voucher.getEndDate().toString(),
                voucher.getQuantity());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }

    @Override
    public void sendPostApprovedEmail(String to, String fullname, String postTitle) {
        String subject = "Bài đăng của bạn đã được phê duyệt";
        String content = String.format("""
                <p>Chào %s,</p>
                <p>Bài đăng <strong>%s</strong> của bạn đã được phê duyệt và hiện đang hiển thị trên hệ thống.</p>
                <p>Cảm ơn bạn đã sử dụng Nhà Trọ Xanh!</p>
                """, fullname, postTitle);
        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendPostRejectedEmail(String to, String fullname, String postTitle) {
        String subject = "Bài đăng của bạn đã bị từ chối";
        String content = String.format("""
                <p>Chào %s,</p>
                <p>Rất tiếc! Bài đăng <strong>%s</strong> của bạn đã bị từ chối.</p>
                <p>Vui lòng kiểm tra lại nội dung và gửi lại nếu cần thiết.</p>
                <p>Trân trọng,<br>Nhà Trọ Xanh</p>
                """, fullname, postTitle);
        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendOwnerApprovalEmail(String to, String fullname) {
        String subject = "Yêu cầu đăng ký chủ trọ đã được phê duyệt";
        String content = String.format(
                """
                        <p>Chào %s,</p>
                        <p>Chúc mừng! Yêu cầu đăng ký làm <strong>chủ trọ</strong> của bạn đã được <strong>phê duyệt</strong>.</p>
                        <p>Bạn có thể đăng nhập và bắt đầu sử dụng chức năng quản lý phòng trọ.</p>
                        <p>Trân trọng,<br>Nhà Trọ Xanh</p>
                        """,
                fullname);

        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendOwnerRejectionEmail(String to, String fullname) {
        String subject = "Yêu cầu đăng ký chủ trọ bị từ chối";
        String content = String.format("""
                <p>Chào %s,</p>
                <p>Rất tiếc! Yêu cầu đăng ký làm <strong>chủ trọ</strong> của bạn đã bị <strong>từ chối</strong>.</p>
                <p>Nếu bạn cần hỗ trợ thêm, vui lòng liên hệ quản trị viên hệ thống.</p>
                <p>Trân trọng,<br>Nhà Trọ Xanh</p>
                """, fullname);

        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendNewPasswordEmail(String to, String fullname, String newPassword) {
        String subject = "Mật khẩu mới của bạn";
        String content = "<p>Chào " + fullname + ",</p>" +
                "<p>Mật khẩu mới của bạn là: <b>" + newPassword + "</b></p>" +
                "<p>Vui lòng sử dụng mật khẩu này để đăng nhập và đổi mật khẩu trong hệ thống nếu cần.</p>" +
                "<p>Trân trọng,<br>Nhà Trọ Xanh</p>";

        sendHtmlMail(to, subject, content);

    }

}
