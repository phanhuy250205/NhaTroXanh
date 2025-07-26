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

    private static final String PRIMARY_COLOR = "#3498DB";
    private static final String SECONDARY_COLOR = "#F8F9FA";
    private static final String TEXT_COLOR = "#333333";
    private static final String LIGHT_TEXT = "#7F8C8D";
    private static final String COMPANY_NAME = "Nhà Trọ Xanh";
    
    @Autowired
    private JavaMailSender mailSender;

    private String getEmailTemplate(String title, String content, String greeting, String footer) {
        return String.format("""
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
        String content = String.format("""
            <p>Hợp đồng thuê của bạn đã chính thức kết thúc.</p>
            <div class="highlight">
                <p><strong>Mã hợp đồng:</strong> %s</p>
                <p><strong>Ngày kết thúc:</strong> %s</p>
            </div>
            <p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi. Chúng tôi hy vọng sẽ được phục vụ bạn trong tương lai.</p>
            """, contractCode, endDate);
        
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

    public void sendContractEmail(String to, String customerName, byte[] pdfContent, String contractNumber) throws Exception {

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

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("nhatroxanh123@gmail.com"); // ✅ Thay bằng email của bạn
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

}

