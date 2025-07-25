package nhatroxanh.com.Nhatroxanh.Service;

import java.sql.Date;

import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;

public interface EmailService {
    void sendExtensionApprovalEmail(String to, String fullname, String contractCode, Date newEndDate);

    void sendExtensionRejectionEmail(String to, String fullname, String contractCode, String reason);

    void sendExpirationWarningEmail(String to, String fullname, String contractCode, Date endDate);

    void sendReturnApprovalEmail(String to, String fullname, String contractCode, Date endDate);

    void sendReturnRejectionEmail(String to, String fullname, String contractCode, String reason);

    void sendContractTerminatedEmail(String to, String fullname, String contractCode, Date endDate);

    void sendVoucherDeactivatedEmail(String to, String fullname, String voucherTitle, String reason);

    void sendIncidentProcessingEmail(String to, IncidentReports incident);

    void sendIncidentResolvedEmail(String to, IncidentReports incident);

    void sendVoucherDeactivationEmail(Vouchers voucher);

    void sendPostApprovedEmail(String to, String fullname, String postTitle);

    void sendPostRejectedEmail(String to, String fullname, String postTitle);

    void sendOwnerApprovalEmail(String to, String fullname);

    void sendOwnerRejectionEmail(String to, String fullname);

    void sendNewPasswordEmail(String to, String fullname, String newPassword);

    void sendSimpleEmail(String to, String subject, String body);
}
