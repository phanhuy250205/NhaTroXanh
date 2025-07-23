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
    // ✅ THÊM METHOD MỚI CHO GỬI HỢP ĐỒNG
    void sendContractEmail(String to, String customerName, byte[] pdfContent, String contractNumber) throws Exception;
    // ✅ THÊM METHOD MỚI ĐƠN GIẢN
    void sendEmail(String to, String subject, String body) throws Exception;

    // ✅ THÊM METHOD GỬI EMAIL VỚI ATTACHMENT
    void sendEmailWithAttachment(String to, String subject, String body, byte[] attachmentData, String fileName) throws Exception;

    // ✅ THÊM METHOD GỬI HỢP ĐỒNG PDF HOÀN CHỈNH
    void sendContractPDF(String to, String tenantName, String roomName, byte[] pdfData, String fileName) throws Exception;

    // ✅ METHOD MỚI GỬI HTML
    void sendContractHtml(String recipientEmail, String recipientName, String subject, String contractHtml);


}
