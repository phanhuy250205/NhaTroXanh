package nhatroxanh.com.Nhatroxanh.Service;

import java.sql.Date;

import nhatroxanh.com.Nhatroxanh.Model.entity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.entity.Vouchers;

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
        void sendContractEmail(String to, String customerName, byte[] pdfContent, String contractNumber)
                        throws Exception;

        // ✅ THÊM METHOD MỚI ĐƠN GIẢN
        void sendEmail(String to, String subject, String body) throws Exception;

        // ✅ THÊM METHOD GỬI EMAIL VỚI ATTACHMENT
        void sendEmailWithAttachment(String to, String subject, String body, byte[] attachmentData, String fileName)
                        throws Exception;

        // ✅ THÊM METHOD GỬI HỢP ĐỒNG PDF HOÀN CHỈNH
        void sendContractPDF(String to, String tenantName, String roomName, byte[] pdfData, String fileName)
                        throws Exception;

        void sendVoucherDeactivationEmail(Vouchers voucher);

        void sendPostApprovedEmail(String to, String fullname, String postTitle);

        void sendPostRejectedEmail(String to, String fullname, String postTitle);

        void sendOwnerApprovalEmail(String to, String fullname);

        void sendOwnerRejectionEmail(String to, String fullname);

        void sendSimpleEmail(String to, String subject, String body);

        void sendNewPasswordEmail(String to, String fullname, String newPassword);

        void sendTextEmail(String to, String subject, String text) throws Exception;

        void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception;

        // Cash payment appointment email methods
        void sendCashPaymentAppointmentEmail(String to, String landlordName, String tenantName,
                        String roomName, String hostelName, String paymentDate,
                        String paymentTime, String amount, String note, Integer paymentId);

        void sendCashPaymentSuccessEmail(String to, String tenantName, String roomName,
                        String hostelName, String amount, String paymentDate, Integer paymentId);

        void sendPrivacyPolicyEmail(String to);

        void sendExtensionRequestEmail(String to, String landlordName, String tenantName, String contractCode,
                        Date requestedExtendDate, String message);

        void sendReturnRequestEmail(String to, String landlordName, String tenantName, String contractCode,
                        Date requestedReturnDate, String returnReason);
}
