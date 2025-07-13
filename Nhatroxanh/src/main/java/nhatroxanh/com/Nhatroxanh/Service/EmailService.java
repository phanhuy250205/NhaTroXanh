package nhatroxanh.com.Nhatroxanh.Service;

import java.sql.Date;

public interface EmailService {
    void sendExtensionApprovalEmail(String to, String fullname, String contractCode, Date newEndDate);

    void sendExtensionRejectionEmail(String to, String fullname, String contractCode, String reason);

    void sendExpirationWarningEmail(String to, String fullname, String contractCode, Date endDate);
}
