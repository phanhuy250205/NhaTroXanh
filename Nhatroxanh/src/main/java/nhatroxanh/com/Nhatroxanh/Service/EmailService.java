package nhatroxanh.com.Nhatroxanh.Service;

import java.sql.Date;

import nhatroxanh.com.Nhatroxanh.Model.entity.IncidentReports;

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

}
