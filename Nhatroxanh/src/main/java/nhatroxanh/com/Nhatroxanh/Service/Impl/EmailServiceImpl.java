package nhatroxanh.com.Nhatroxanh.Service.Impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.text.SimpleDateFormat;

@Service
public class EmailServiceImpl implements EmailService {

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
