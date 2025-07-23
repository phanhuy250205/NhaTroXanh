package nhatroxanh.com.Nhatroxanh.Service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentRequestDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentResponseDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments.PaymentStatus;
import nhatroxanh.com.Nhatroxanh.Repository.*;
import nhatroxanh.com.Nhatroxanh.Service.PaymentService;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentsRepository paymentsRepository;
    private final DetailPaymentsRepository detailPaymentsRepository;
    private final ContractsRepository contractsRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    // NumberFormat for Vietnamese currency
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final int MAX_NOTIFICATION_ATTEMPTS_PER_DAY = 2;

    @Override
    public List<PaymentResponseDto> getPaymentsByOwnerId(Integer ownerId) {
        List<Payments> payments = paymentsRepository.findByOwnerId(ownerId);
        return payments.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PaymentResponseDto> getPaymentsByOwnerIdWithPagination(Integer ownerId, Pageable pageable) {
        Page<Payments> paymentsPage = paymentsRepository.findByOwnerIdWithPagination(ownerId, pageable);
        List<PaymentResponseDto> paymentDtos = paymentsPage.getContent().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
        return new PageImpl<>(paymentDtos, pageable, paymentsPage.getTotalElements());
    }

    @Override
    public List<PaymentResponseDto> getRecentPaymentsByOwnerId(Integer ownerId) {
        Pageable pageable = PageRequest.of(0, 8);
        List<Payments> payments = paymentsRepository.findTop8ByOwnerIdOrderByDueDateDesc(ownerId, pageable);
        return payments.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByOwnerIdAndStatus(Integer ownerId, PaymentStatus status) {
        List<Payments> payments = paymentsRepository.findByOwnerIdAndStatus(ownerId, status);
        return payments.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PaymentResponseDto> getPaymentsByOwnerIdAndStatusWithPagination(Integer ownerId, PaymentStatus status,
            Pageable pageable) {
        Page<Payments> paymentsPage = paymentsRepository.findByOwnerIdAndStatusWithPagination(ownerId, status,
                pageable);
        List<PaymentResponseDto> paymentDtos = paymentsPage.getContent().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
        return new PageImpl<>(paymentDtos, pageable, paymentsPage.getTotalElements());
    }

    @Override
    public List<PaymentResponseDto> searchPayments(Integer ownerId, String keyword) {
        List<Payments> payments = paymentsRepository.searchPaymentsByKeyword(ownerId, keyword);
        return payments.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PaymentResponseDto> searchPaymentsWithPagination(Integer ownerId, String keyword, Pageable pageable) {
        Page<Payments> paymentsPage = paymentsRepository.searchPaymentsByKeywordWithPagination(ownerId, keyword,
                pageable);
        List<PaymentResponseDto> paymentDtos = paymentsPage.getContent().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
        return new PageImpl<>(paymentDtos, pageable, paymentsPage.getTotalElements());
    }

    @Override
    public PaymentResponseDto getPaymentById(Integer paymentId) {
        Payments payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
        return convertToResponseDto(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        try {
            Contracts contract = contractsRepository.findById(request.getContractId())
                    .orElseThrow(() -> new RuntimeException("Contract not found with id: " + request.getContractId()));

            String[] monthYear = request.getMonth().split("/");
            int month = Integer.parseInt(monthYear[0]);
            int year = Integer.parseInt(monthYear[1]);

            Optional<Payments> existingPayment = paymentsRepository.findByContractIdAndMonth(
                    request.getContractId(), month, year);
            if (existingPayment.isPresent()) {
                throw new RuntimeException("Payment already exists for this month");
            }

            if (request.getDetails() == null || request.getDetails().isEmpty()) {
                throw new RuntimeException("Payment details cannot be empty");
            }

            Float totalAmount = 0f;
            for (PaymentRequestDto.PaymentDetailDto detail : request.getDetails()) {
                if (detail.getAmount() == null || detail.getAmount() < 0) {
                    throw new RuntimeException("Invalid amount for item: " + detail.getItemName());
                }
                totalAmount += detail.getAmount();
            }

            Payments payment = Payments.builder()
                    .contract(contract)
                    .totalAmount(totalAmount)
                    .dueDate(request.getDueDate())
                    .paymentStatus(PaymentStatus.CHƯA_THANH_TOÁN)
                    .paymentMethod(request.getPaymentMethod())
                    .notificationAttemptsToday(0)
                    .lastNotificationDate(null)
                    .build();

            payment = paymentsRepository.save(payment);

            for (PaymentRequestDto.PaymentDetailDto detail : request.getDetails()) {
                DetailPayments detailPayment = DetailPayments.builder()
                        .payment(payment)
                        .itemName(detail.getItemName())
                        .quantity(detail.getQuantity())
                        .unitPrice(detail.getUnitPrice())
                        .amountUnitPrice(detail.getAmount())
                        .build();
                detailPaymentsRepository.save(detailPayment);
            }

            log.info("Created payment with id: {} for contract: {} with payment method: {}",
                    payment.getId(), contract.getContractId(), payment.getPaymentMethod());
            return convertToResponseDto(payment);

        } catch (Exception e) {
            log.error("Error creating payment: ", e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponseDto updatePaymentStatus(Integer paymentId, PaymentStatus status) {
        Payments payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        payment.setPaymentStatus(status);
        if (status == PaymentStatus.ĐÃ_THANH_TOÁN) {
            payment.setPaymentDate(Date.valueOf(LocalDate.now()));
        }

        payment = paymentsRepository.save(payment);
        log.info("Updated payment status to {} for payment id: {}", status, paymentId);
        return convertToResponseDto(payment);
    }

    @Override
    public Map<String, Object> getPaymentStatistics(Integer ownerId) {
        Map<String, Object> stats = new HashMap<>();

        long paidCount = paymentsRepository.countPaidPaymentsByOwnerId(ownerId);
        long unpaidCount = paymentsRepository.countUnpaidPaymentsByOwnerId(ownerId);
        Float totalRevenue = paymentsRepository.getTotalRevenueByOwnerId(ownerId);

        stats.put("paidCount", paidCount);
        stats.put("unpaidCount", unpaidCount);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0f);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getAvailableContractsForPayment(Integer ownerId) {
        try {
            List<Contracts> contracts = contractRepository.findByOwnerId(ownerId);

            List<Contracts> activeContracts = contracts.stream()
                    .filter(contract -> contract.getStatus() == Contracts.Status.ACTIVE)
                    .collect(Collectors.toList());

            List<Map<String, Object>> result = new ArrayList<>();

            for (Contracts contract : activeContracts) {
                Map<String, Object> contractData = new HashMap<>();
                contractData.put("contractId", contract.getContractId());
                contractData.put("roomCode", contract.getRoom().getNamerooms());
                contractData.put("hostelName", contract.getRoom().getHostel().getName());
                contractData.put("roomPrice", contract.getPrice());
                String tenantName = "";
                if (contract.getTenant() != null) {
                    tenantName = contract.getTenant().getFullname();
                } else if (contract.getUnregisteredTenant() != null) {
                    tenantName = contract.getUnregisteredTenant().getFullName();
                }
                contractData.put("tenantName", tenantName);
                contractData.put("tenantPhone", contract.getTenantPhone());
                contractData.put("roomId", contract.getRoom().getRoomId());

                result.add(contractData);
            }

            log.info("Found {} available contracts for owner {}", result.size(), ownerId);
            return result;

        } catch (Exception e) {
            log.error("Error getting available contracts for payment: ", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> calculateUtilityCosts(Integer previousReading, Integer currentReading,
            String utilityType, Float unitPrice) {
        Map<String, Object> result = new HashMap<>();

        int usage = currentReading - previousReading;
        Float totalCost = usage * unitPrice;

        result.put("usage", usage);
        result.put("unit", utilityType.equals("electricity") ? "kWh" : "m³");
        result.put("unitPrice", unitPrice);
        result.put("totalCost", totalCost);
        result.put("utilityType", utilityType);

        return result;
    }

    @Override
    public void deletePayment(Integer paymentId) {
        try {
            Payments payment = paymentsRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

            detailPaymentsRepository.deleteByPaymentId(paymentId);
            paymentsRepository.delete(payment);

            log.info("Deleted payment with id: {}", paymentId);
        } catch (Exception e) {
            log.error("Error deleting payment with id {}: ", paymentId, e);
            throw new RuntimeException("Failed to delete payment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public int sendInvoicesToTenants(List<PaymentResponseDto> payments) {
        if (payments == null || payments.isEmpty()) {
            log.info("No invoices to send");
            return 0;
        }

        int sentCount = 0;
        LocalDate today = LocalDate.now();

        for (PaymentResponseDto payment : payments) {
            boolean emailSent = false;
            boolean notificationCreated = false;

            try {
                // Retrieve payment entity to check and update notification attempts
                Payments paymentEntity = paymentsRepository.findById(payment.getPaymentId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Payment not found for ID: " + payment.getPaymentId()));

                // Check and reset notification attempts if it's a new day
                if (paymentEntity.getLastNotificationDate() == null ||
                        !paymentEntity.getLastNotificationDate().toLocalDate().equals(today)) {
                    paymentEntity.setNotificationAttemptsToday(0);
                    paymentEntity.setLastNotificationDate(Date.valueOf(today));
                    log.info("Reset notification attempts for invoice {} to 0 for new day: {}",
                            payment.getPaymentId(), today);
                }

                // Check daily notification limit
                if (paymentEntity.getNotificationAttemptsToday() >= MAX_NOTIFICATION_ATTEMPTS_PER_DAY) {
                    log.warn("Daily notification limit reached for invoice {} (attempts: {}). Skipping.",
                            payment.getPaymentId(), paymentEntity.getNotificationAttemptsToday());
                    continue;
                }

                // Retrieve contract associated with the payment
                Contracts contract = contractsRepository.findById(payment.getContractId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Contract not found for payment ID: " + payment.getPaymentId()));

                // Retrieve tenant (user) associated with the contract
                Users tenant = null;
                if (contract.getTenant() != null) {
                    tenant = userRepository.findById(contract.getTenant().getUserId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Tenant not found for contract ID: " + contract.getContractId()));
                } else if (contract.getUnregisteredTenant() != null) {
                    log.warn(
                            "Unregistered tenant for contract ID: {} for payment ID: {}. Email and notification skipped.",
                            contract.getContractId(), payment.getPaymentId());
                    continue;
                } else {
                    log.warn("No tenant found for contract ID: {} for payment ID: {}",
                            contract.getContractId(), payment.getPaymentId());
                    continue;
                }

                // Create in-app notification
                try {
                    createPaymentNotification(tenant, payment);
                    notificationCreated = true;
                    log.info("Notification created for invoice {} for tenant {}", payment.getPaymentId(),
                            tenant.getUserId());
                } catch (Exception e) {
                    log.error("Failed to create notification for invoice {} for tenant {}: {}",
                            payment.getPaymentId(), tenant.getUserId(), e.getMessage());
                }

                // Send email if tenant has an email
                if (tenant.getEmail() != null && !tenant.getEmail().isEmpty()) {
                    try {
                        String subject = "Hóa Đơn Thanh Toán #" + payment.getPaymentId() + " - " + payment.getMonth();
                        String body = buildEmailBody(payment);
                        sendEmail(tenant.getEmail(), subject, body);
                        emailSent = true;
                        log.info("Email sent for invoice {} to tenant {} (email: {})",
                                payment.getPaymentId(), tenant.getUserId(), tenant.getEmail());
                    } catch (MailAuthenticationException e) {
                        log.error("Email authentication failed for invoice {} to tenant {}: {}",
                                payment.getPaymentId(), tenant.getUserId(), e.getMessage());
                    } catch (Exception e) {
                        log.error("Failed to send email for invoice {} to tenant {}: {}",
                                payment.getPaymentId(), tenant.getUserId(), e.getMessage());
                    }
                } else {
                    log.warn("No email found for tenant ID: {} for payment ID: {}", tenant.getUserId(),
                            payment.getPaymentId());
                }

                // Update notification attempts if either email or notification was successful
                if (emailSent || notificationCreated) {
                    paymentEntity.setNotificationAttemptsToday(paymentEntity.getNotificationAttemptsToday() + 1);
                    paymentEntity.setLastNotificationDate(Date.valueOf(today));
                    paymentsRepository.save(paymentEntity);
                    sentCount++;
                    log.info(
                            "Successfully processed invoice {} for tenant {} (email: {}, notification: {}, attempts today: {})",
                            payment.getPaymentId(), tenant.getUserId(), emailSent, notificationCreated,
                            paymentEntity.getNotificationAttemptsToday());
                } else {
                    log.warn("No actions (email or notification) completed for invoice {} for tenant {}",
                            payment.getPaymentId(), tenant.getUserId());
                }

            } catch (Exception e) {
                log.error("Failed to process invoice {}: {}", payment.getPaymentId(), e.getMessage());
            }
        }

        log.info("Sent {} out of {} invoices", sentCount, payments.size());
        return sentCount;
    }

    /**
     * Builds a styled HTML email body for the invoice.
     * 
     * @param payment Payment details
     * @return HTML email body
     */
    private String buildEmailBody(PaymentResponseDto payment) {
        StringBuilder body = new StringBuilder();
        body.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>")
                .append("<meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<style>")
                .append("body { font-family: Arial, Helvetica, sans-serif; color: #333; max-width: 600px; margin: 20px auto; padding: 0 10px; }")
                .append(".container { border: 1px solid #e0e0e0; border-radius: 8px; padding: 20px; background-color: #f9f9f9; }")
                .append("h2 { color: #2c3e50; text-align: center; font-size: 24px; margin: 0 0 20px; }")
                .append(".header { background-color: #3498db; color: white; padding: 15px; text-align: center; border-radius: 8px 8px 0 0; }")
                .append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }")
                .append("th, td { padding: 12px; border: 1px solid #ddd; text-align: left; font-size: 14px; }")
                .append("th { background-color: #ecf0f1; font-weight: bold; }")
                .append(".total { font-weight: bold; font-size: 16px; text-align: right; }")
                .append(".footer { text-align: center; color: #777; font-size: 12px; margin-top: 20px; }")
                .append(".payment-button { display: inline-block; padding: 10px 20px; background-color: #28a745; color: white; text-decoration: none; border-radius: 5px; font-size: 14px; margin: 20px 0; }")
                .append(".status-badge { display: inline-block; padding: 6px 12px; border-radius: 12px; font-size: 12px; font-weight: bold; }")
                .append(".status-paid { background-color: #d4edda; color: #155724; }")
                .append(".status-pending { background-color: #fff3cd; color: #856404; }")
                .append(".status-overdue { background-color: #f8d7da; color: #721c24; }")
                .append("@media (max-width: 600px) { .container { padding: 10px; } th, td { font-size: 12px; padding: 8px; } }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class='container'>")
                .append("<div class='header'>")
                .append("<h2>Hóa Đơn Thanh Toán</h2>")
                .append("</div>")
                .append("<p><strong>Mã hóa đơn:</strong> ").append(payment.getPaymentId()).append("</p>")
                .append("<p><strong>Tháng:</strong> ").append(payment.getMonth()).append("</p>")
                .append("<p><strong>Hạn thanh toán:</strong> ").append(payment.getDueDate()).append("</p>")
                .append("<p><strong>Trạng thái:</strong> <span class='status-badge ");

        // Handle payment status safely
        String status = payment.getPaymentStatus() != null ? payment.getPaymentStatus().toString().toLowerCase()
                : "unknown";
        if (status.contains("đã_thanh_toán")) {
            body.append("status-paid'>Đã thanh toán");
        } else if (status.contains("quá_hạn_thanh_toán")) {
            body.append("status-overdue'>Quá hạn thanh toán");
        } else {
            body.append("status-pending'>Chưa thanh toán");
        }
        body.append("</span></p>")
                .append("<table>")
                .append("<tr><th>Khoản mục</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr>");

        for (PaymentResponseDto.PaymentDetailResponseDto detail : payment.getDetails()) {
            body.append("<tr>")
                    .append("<td>").append(detail.getItemName()).append(" (").append(detail.getDisplayText())
                    .append(")</td>")
                    .append("<td>").append(detail.getQuantity()).append("</td>")
                    .append("<td>").append(CURRENCY_FORMAT.format(detail.getUnitPrice())).append(" VNĐ</td>")
                    .append("<td>").append(CURRENCY_FORMAT.format(detail.getAmount())).append(" VNĐ</td>")
                    .append("</tr>");
        }

        body.append("</table>")
                .append("<p class='total'>Tổng cộng: ").append(CURRENCY_FORMAT.format(payment.getTotalAmount()))
                .append(" VNĐ</p>")
                .append("<a href='/tenant/payments/").append(payment.getPaymentId())
                .append("' class='payment-button'>Thanh toán ngay</a>")
                .append("<p>Vui lòng thanh toán trước ngày đến hạn. Liên hệ chủ trọ nếu có thắc mắc.</p>")
                .append("<div class='footer'>")
                .append("Nhà Trọ Xanh - Hỗ trợ: <a href='mailto:support@nhatroxanh.com'>support@nhatroxanh.com</a>")
                .append("</div>")
                .append("</div>")
                .append("</body>")
                .append("</html>");

        return body.toString();
    }

    /**
     * Sends an email with the specified subject and HTML body.
     * 
     * @param to      Recipient email address
     * @param subject Email subject
     * @param body    HTML email body
     */
    private void sendEmail(String to, String subject, String body) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        helper.setFrom("no-reply@nhatroxanh.com");

        mailSender.send(message);
        log.debug("Email sent to {}", to);
    }

    /**
     * Creates an in-app notification for a payment reminder with detailed invoice
     * information.
     * 
     * @param tenant  Tenant user
     * @param payment Payment details
     */
    private void createPaymentNotification(Users tenant, PaymentResponseDto payment) {
        StringBuilder message = new StringBuilder();
        message.append("Hóa đơn #").append(payment.getPaymentId())
                .append(" cho tháng ").append(payment.getMonth())
                .append(" (Tổng: ").append(CURRENCY_FORMAT.format(payment.getTotalAmount())).append(" VNĐ).")
                .append(" Hạn thanh toán: ").append(payment.getDueDate()).append(". Chi tiết: ");

        List<String> detailStrings = payment.getDetails().stream()
                .map(detail -> String.format("%s: %s VNĐ (%s)",
                        detail.getItemName(), CURRENCY_FORMAT.format(detail.getAmount()), detail.getDisplayText()))
                .collect(Collectors.toList());
        message.append(String.join(", ", detailStrings)).append(".");

        String finalMessage = message.length() > 2000 ? message.substring(0, 1997) + "..." : message.toString();

        // Lấy Contracts để gán room
        Contracts contract = contractsRepository.findById(payment.getContractId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contract not found for payment ID: " + payment.getPaymentId()));
        Rooms room = contract.getRoom();

        Notification notification = Notification.builder()
                .user(tenant)
                .title("Nhắc nhở thanh toán")
                .message(finalMessage)
                .type(Notification.NotificationType.PAYMENT)
                .isRead(false)
                .createAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
                .room(room) // Gán room từ Contracts
                .build();

        notificationRepository.save(notification);
        log.info("Created notification for user {} for payment {} with room_id {}: {}",
                tenant.getUserId(), payment.getPaymentId(), room.getRoomId(), finalMessage);
    }

    private PaymentResponseDto convertToResponseDto(Payments payment) {
        Contracts contract = payment.getContract();
        Rooms room = contract.getRoom();

        String tenantName = "";
        String tenantPhone = contract.getTenantPhone();
        if (contract.getTenant() != null) {
            tenantName = contract.getTenant().getFullname();
        } else if (contract.getUnregisteredTenant() != null) {
            tenantName = contract.getUnregisteredTenant().getFullName();
        }

        String month = "";
        if (payment.getDueDate() != null) {
            LocalDate dueDate = payment.getDueDate().toLocalDate();
            month = String.format("%02d/%d", dueDate.getMonthValue(), dueDate.getYear());
        }

        List<DetailPayments> details = detailPaymentsRepository.findByPaymentId(payment.getId());
        List<PaymentResponseDto.PaymentDetailResponseDto> detailDtos = details.stream()
                .map(detail -> PaymentResponseDto.PaymentDetailResponseDto.builder()
                        .detailId(detail.getDetailId())
                        .itemName(detail.getItemName())
                        .quantity(detail.getQuantity())
                        .unitPrice(detail.getUnitPrice())
                        .amount(detail.getAmountUnitPrice())
                        .displayText(formatDisplayText(detail))
                        .build())
                .collect(Collectors.toList());

        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .contractId(contract.getContractId())
                .roomCode(room.getNamerooms())
                .hostelName(room.getHostel().getName())
                .tenantName(tenantName)
                .tenantPhone(tenantPhone)
                .month(month)
                .totalAmount(payment.getTotalAmount())
                .dueDate(payment.getDueDate())
                .paymentDate(payment.getPaymentDate())
                .paymentStatus(payment.getPaymentStatus())
                .paymentMethod(payment.getPaymentMethod())
                .details(detailDtos)
                .build();
    }

    private String formatDisplayText(DetailPayments detail) {
        if (detail.getItemName().toLowerCase().contains("điện")) {
            return detail.getQuantity() + "kWh";
        } else if (detail.getItemName().toLowerCase().contains("nước")) {
            return detail.getQuantity() + " m³";
        }
        return detail.getQuantity() != null ? detail.getQuantity().toString() : "";
    }
}
