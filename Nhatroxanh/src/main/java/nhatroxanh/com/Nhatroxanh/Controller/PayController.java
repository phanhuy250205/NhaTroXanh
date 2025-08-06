package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.Address;
import nhatroxanh.com.Nhatroxanh.Model.entity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.entity.DetailPayments;
import nhatroxanh.com.Nhatroxanh.Model.entity.District;
import nhatroxanh.com.Nhatroxanh.Model.entity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Model.entity.Ward;
import nhatroxanh.com.Nhatroxanh.Repository.AddressRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.DetailPaymentsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.PaymentsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.VoucherRepository;
import nhatroxanh.com.Nhatroxanh.Service.VoucherService;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import nhatroxanh.com.Nhatroxanh.Service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpSession;

import java.sql.Date;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PayController {

    private final PaymentsRepository paymentsRepository;
    private final DetailPaymentsRepository detailPaymentsRepository;
    private final ContractsRepository contractsRepository;
    private final RoomsRepository roomsRepository;
    private final AddressRepository addressRepository;
    private final VoucherRepository voucherRepository;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private NotificationService notificationService;

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @GetMapping("/thanh-toan")
    @Transactional
    public String viewPaymentPage(
            @RequestParam("invoiceId") String invoiceId,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            Model model,
            HttpSession session) {
        try {
            log.info("Loading payment page with invoiceId={}, room_id={}, hostel_id={} at {}",
                    invoiceId, roomId, hostelId, LocalDateTime.now());

            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt)
                    .orElseThrow(() -> {
                        log.error("Payment not found with id: {} at {}", invoiceId, LocalDateTime.now());
                        return new IllegalArgumentException("Payment not found with id: " + invoiceId);
                    });

            // Clear any existing voucher session data
            session.removeAttribute("originalTotal_" + invoiceId);
            session.removeAttribute("discountAmount_" + invoiceId);
            session.removeAttribute("voucherCode_" + invoiceId);

            // Remove any existing voucher discount from payment details
            List<DetailPayments> details = detailPaymentsRepository.findByPaymentId(paymentIdInt);
            Float originalTotal = payment.getTotalAmount().floatValue();
            Float discountAmount = 0f;
            for (DetailPayments detail : details) {
                if (detail.getItemName().toLowerCase().startsWith("giảm giá voucher ")) {
                    discountAmount += Math.abs(detail.getAmountUnitPrice());
                    detailPaymentsRepository.delete(detail);
                    log.info("Removed existing voucher discount for payment {} on page load", invoiceId);
                }
            }
            if (discountAmount > 0) {
                payment.setTotalAmount(Double.valueOf(originalTotal + discountAmount));
                paymentsRepository.save(payment);
                log.info("Reverted payment total for invoiceId {} to {} due to voucher removal", invoiceId,
                        payment.getTotalAmount());
            }
            Rooms room;
            if (roomId != null && hostelId != null) {
                room = roomsRepository.findById(roomId)
                        .filter(r -> r.getHostel() != null && r.getHostel().getHostelId().equals(hostelId))
                        .orElseThrow(() -> {
                            log.error("Room not found or hostel mismatch for room_id: {} at {}", roomId,
                                    LocalDateTime.now());
                            return new IllegalArgumentException(
                                    "Room not found or hostel mismatch for room_id: " + roomId);
                        });
            } else {
                Contracts contract = contractsRepository.findById(payment.getContract().getContractId())
                        .orElseThrow(() -> {
                            log.error("Contract not found for payment ID: {} at {}", invoiceId, LocalDateTime.now());
                            return new IllegalArgumentException("Contract not found for payment ID: " + invoiceId);
                        });
                room = roomsRepository.findById(contract.getRoom().getRoomId())
                        .orElseThrow(() -> {
                            log.error("Room not found for contract ID: {} at {}", contract.getContractId(),
                                    LocalDateTime.now());
                            return new IllegalArgumentException(
                                    "Room not found for contract ID: " + contract.getContractId());
                        });
            }

            // Use String address directly
            String fullAddress = room.getHostel() != null && room.getHostel().getAddress() != null
                    ? room.getHostel().getAddress()
                    : "N/A";

            // Set model attributes
            model.addAttribute("invoiceId", invoiceId);
            model.addAttribute("totalAmount", String.format("%,d VNĐ", payment.getTotalAmount().intValue()));
            model.addAttribute("originalTotal", String.format("%,d VNĐ", originalTotal.intValue()));
            model.addAttribute("discountAmount", String.format("%,d VNĐ", discountAmount.intValue()));
            model.addAttribute("voucherCode", null); // No voucher applied initially
            model.addAttribute("roomPrice", Optional.ofNullable(room.getPrice())
                    .map(price -> String.format("%,d VNĐ", price.intValue()))
                    .orElse("0 VNĐ"));
            model.addAttribute("roomName", Optional.ofNullable(room).map(Rooms::getNamerooms).orElse("Không xác định"));
            model.addAttribute("hostName", Optional.ofNullable(room)
                    .map(Rooms::getHostel)
                    .map(hostel -> hostel.getOwner())
                    .map(owner -> owner.getFullname())
                    .orElse("Không xác định"));
            model.addAttribute("dueDate", Optional.ofNullable(payment.getDueDate())
                    .map(date -> date.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .orElse("Không xác định"));
            model.addAttribute("month", Optional.ofNullable(payment.getDueDate())
                    .map(date -> String.format("Tháng %02d/%d", date.toLocalDate().getMonthValue(),
                            date.toLocalDate().getYear()))
                    .orElse("Không xác định"));
            model.addAttribute("hostelAddress", fullAddress);

            // Handle payment status
            String status;
            Payments.PaymentStatus paymentStatus = payment.getPaymentStatus();
            if (paymentStatus == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                status = "PAID";
            } else if (paymentStatus == Payments.PaymentStatus.QUÁ_HẠN_THANH_TOÁN) {
                status = "OVERDUE";
            } else {
                status = "PENDING";
            }
            model.addAttribute("status", status);

            model.addAttribute("paymentMethod",
                    Optional.ofNullable(payment.getPaymentMethod()).map(Object::toString).orElse(""));
            model.addAttribute("paymentDate", Optional.ofNullable(payment.getPaymentDate())
                    .map(date -> date.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .orElse("N/A"));

            // Initialize utility defaults
            model.addAttribute("electricUsage", "0 kWh");
            model.addAttribute("electricCost", "0 VNĐ");
            model.addAttribute("electricUnitPrice", "0 VNĐ");
            model.addAttribute("prevElectricReading", "Không xác định");
            model.addAttribute("currElectricReading", "Không xác định");
            model.addAttribute("waterUsage", "0 m³");
            model.addAttribute("waterCost", "0 VNĐ");
            model.addAttribute("waterUnitPrice", "0 VNĐ");
            model.addAttribute("prevWaterReading", "Không xác định");
            model.addAttribute("currWaterReading", "Không xác định");
            model.addAttribute("serviceFee", "0 VNĐ");

            details.forEach(detail -> {
                try {
                    String itemNameLower = detail.getItemName().toLowerCase();
                    Double quantity = Optional.ofNullable(detail.getQuantity()).orElse(0).doubleValue();
                    Double unitPrice = Optional.ofNullable(detail.getUnitPrice()).orElse(0f).doubleValue();
                    Double amountUnitPrice = Optional.ofNullable(detail.getAmountUnitPrice()).orElse(0f).doubleValue();

                    if (itemNameLower.contains("điện") || itemNameLower.contains("dien")
                            || itemNameLower.contains("electric")) {
                        model.addAttribute("electricUsage", String.format("%.0f kWh", quantity));
                        model.addAttribute("electricCost", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                        model.addAttribute("electricUnitPrice", String.format("%,d VNĐ", unitPrice.intValue()));
                        Double prevElectric = 0.0; // Replace with actual previous reading if available
                        model.addAttribute("prevElectricReading", String.format("%.0f", prevElectric));
                        model.addAttribute("currElectricReading", String.format("%.0f", prevElectric + quantity));
                    } else if (itemNameLower.contains("nước") || itemNameLower.contains("nuoc")
                            || itemNameLower.contains("water")) {
                        model.addAttribute("waterUsage", String.format("%.0f m³", quantity));
                        model.addAttribute("waterCost", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                        model.addAttribute("waterUnitPrice", String.format("%,d VNĐ", unitPrice.intValue()));
                        Double prevWater = 0.0; // Replace with actual previous reading if available
                        model.addAttribute("prevWaterReading", String.format("%.0f", prevWater));
                        model.addAttribute("currWaterReading", String.format("%.0f", prevWater + quantity));
                    } else if (itemNameLower.contains("dịch vụ") || itemNameLower.contains("dich vu")
                            || itemNameLower.contains("phí") || itemNameLower.contains("phi")
                            || itemNameLower.contains("service") || itemNameLower.contains("fee")) {
                        model.addAttribute("serviceFee", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                    }
                } catch (Exception e) {
                    log.error("Error processing detail payment item: {} at {}", detail.getItemName(),
                            LocalDateTime.now(), e);
                }
            });

            log.info("Payment page loaded successfully for invoiceId: {}", invoiceId);
            return "guest/thanh-toan";
        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {} at {}", invoiceId, LocalDateTime.now(), e);
            model.addAttribute("error", "Mã hóa đơn không hợp lệ: " + invoiceId);
            return "guest/thanh-toan";
        } catch (IllegalArgumentException e) {
            log.error("Entity not found for invoiceId: {} at {}", invoiceId, LocalDateTime.now(), e);
            model.addAttribute("error", e.getMessage());
            return "guest/thanh-toan";
        } catch (Exception e) {
            log.error("Unexpected error loading payment page at {}: {}", LocalDateTime.now(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi hệ thống: Vui lòng thử lại sau.");
            return "guest/thanh-toan";
        }
    }

    @PostMapping("/thanh-toan")
    @Transactional
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestParam("invoiceId") String invoiceId,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "wallet", required = false) String wallet,
            @RequestParam(value = "paymentDate", required = false) String paymentDate,
            @RequestParam(value = "paymentTime", required = false) String paymentTime,
            @RequestParam(value = "paymentNote", required = false) String paymentNote,
            @RequestParam(value = "voucherCode", required = false) String voucherCode,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Processing payment with invoiceId={}, room_id={}, hostel_id={}, address_id={}, method={} at {}",
                    invoiceId, roomId, hostelId, addressId, paymentMethod, LocalDateTime.now());
            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt)
                    .orElseThrow(() -> new IllegalArgumentException("Hóa đơn không tồn tại với mã: " + invoiceId));

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                throw new IllegalStateException("Hóa đơn đã được thanh toán.");
            }

            // Apply voucher if provided
            Float discount = 0f;
            String appliedVoucherCode = null;
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                List<DetailPayments> details = detailPaymentsRepository.findByPaymentId(paymentIdInt);
                boolean alreadyApplied = details.stream()
                        .anyMatch(detail -> detail.getItemName().toLowerCase().contains("giảm giá voucher"));
                if (!alreadyApplied) {
                    Vouchers voucher = voucherService.getVoucherByCode(voucherCode);
                    if (voucher != null && voucher.getStatus() && voucher.getQuantity() > 0 &&
                            (voucher.getEndDate() == null
                                    || !voucher.getEndDate().before(new Date(System.currentTimeMillis())))
                            &&
                            (voucher.getMinAmount() == null || payment.getTotalAmount() >= voucher.getMinAmount())) {

                        discount = voucher.getDiscountValue();
                        if (payment.getTotalAmount() - discount < 0) {
                            discount = payment.getTotalAmount().floatValue();
                        }

                        // Update totalAmount
                        payment.setTotalAmount(payment.getTotalAmount() - discount);
                        paymentsRepository.save(payment);

                        // Save discount detail
                        DetailPayments discountDetail = DetailPayments.builder()
                                .payment(payment)
                                .itemName("Giảm giá voucher " + voucherCode)
                                .quantity(1)
                                .unitPrice(-discount)
                                .amountUnitPrice(-discount)
                                .build();
                        detailPaymentsRepository.save(discountDetail);

                        // Store voucher info in session
                        session.setAttribute("originalTotal_" + invoiceId, payment.getTotalAmount() + discount);
                        session.setAttribute("discountAmount_" + invoiceId, discount);
                        session.setAttribute("voucherCode_" + invoiceId, voucherCode);
                        appliedVoucherCode = voucherCode;

                        log.info("Applied voucher {} with discount {} for payment {} during processPayment",
                                voucherCode, discount, invoiceId);
                    } else {
                        log.warn("Invalid voucher {} for payment {} during processPayment", voucherCode, invoiceId);
                        if (voucher != null && voucher.getQuantity() <= 0) {
                            throw new IllegalArgumentException("Voucher đã hết số lượng");
                        } else if (voucher != null && !voucher.getStatus()) {
                            throw new IllegalArgumentException("Voucher đã hết hiệu lực");
                        }
                    }
                }
            }

            Payments.PaymentMethod methodEnum = determinePaymentMethod(paymentMethod, wallet);
            payment.setPaymentMethod(methodEnum);

            if ("cash".equalsIgnoreCase(paymentMethod) && paymentDate != null && paymentTime != null) {
                // Set payment status to waiting for cash payment confirmation
                payment.setPaymentStatus(Payments.PaymentStatus.CHỜ_XÁC_NHẬN_TIỀN_MẶT);
                // Save scheduled payment date, time, and note
                payment.setScheduledPaymentDate(Date.valueOf(paymentDate));
                payment.setScheduledPaymentTime(paymentTime);
                payment.setPaymentNote(paymentNote);
                payment.setLandlordNotified(false);
                paymentsRepository.save(payment);

                // Send email to landlord with appointment details
                Contracts contract = payment.getContract();
                if (contract != null && contract.getRoom() != null && contract.getRoom().getHostel() != null) {
                    String landlordEmail = contract.getRoom().getHostel().getOwner().getEmail();
                    String landlordName = contract.getRoom().getHostel().getOwner().getFullname();
                    String tenantName = contract.getTenant() != null ? contract.getTenant().getFullname() : "Khách thuê";
                    String roomName = contract.getRoom().getNamerooms();
                    String hostelName = contract.getRoom().getHostel().getName();
                    String amount = CURRENCY_FORMAT.format(payment.getTotalAmount()) + " VNĐ";

                    try {
                        emailService.sendCashPaymentAppointmentEmail(
                            landlordEmail, landlordName, tenantName, roomName, hostelName,
                            paymentDate, paymentTime, amount, paymentNote, payment.getId()
                        );
                        log.info("Sent cash payment appointment email to landlord {}", landlordEmail);
                    } catch (Exception e) {
                        log.error("Failed to send cash payment appointment email: {}", e.getMessage());
                    }

                    // Create notification for tenant about the appointment
                    if (contract.getTenant() != null) {
                        try {
                            notificationService.createCashPaymentAppointmentNotification(
                                contract.getTenant(), payment, paymentDate, paymentTime, paymentNote
                            );
                            log.info("Created cash payment appointment notification for tenant {}", 
                                    contract.getTenant().getUserId());
                        } catch (Exception e) {
                            log.error("Failed to create cash payment appointment notification: {}", e.getMessage());
                        }
                    }
                }

                response.put("success", true);
                response.put("message", "Đã đặt lịch thanh toán tiền mặt thành công! Chủ trọ sẽ liên hệ để xác nhận. Vui lòng chờ xác nhận từ chủ trọ.");
                response.put("appointmentScheduled", true);
                // Không redirect, giữ nguyên trang thanh toán
                return ResponseEntity.ok(response);
            } else {
                // For other payment methods, process as paid immediately
                payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
                payment.setPaymentDate(new java.sql.Timestamp(System.currentTimeMillis()));
                paymentsRepository.save(payment);

                // Decrease voucher quantity after successful payment
                decreaseVoucherQuantity(paymentIdInt, invoiceId);

                // Pass voucher code to success page
                if (appliedVoucherCode != null) {
                    session.setAttribute("voucherCode_" + invoiceId, appliedVoucherCode);
                }

                response.put("success", true);
                response.put("message", "Thanh toán thành công! Giảm giá: " + discount + " VNĐ");
                response.put("redirectUrl", "/guest/success-thanhtoan?invoiceId=" + invoiceId);
                return ResponseEntity.ok(response);
            }

        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {} at {}", invoiceId, LocalDateTime.now(), e);
            response.put("success", false);
            response.put("error", "Mã hóa đơn không hợp lệ.");
            response.put("failureUrl",
                    "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Mã hóa đơn không hợp lệ");
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error processing payment for invoice {} at {}: {}", invoiceId, LocalDateTime.now(),
                    e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("failureUrl",
                    "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (OptimisticLockException e) {
            log.error("Concurrent update conflict for voucher in payment {} at {}", invoiceId, LocalDateTime.now(), e);
            response.put("success", false);
            response.put("error", "Lỗi do xung đột dữ liệu voucher. Vui lòng thử lại.");
            response.put("failureUrl",
                    "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Lỗi do xung đột dữ liệu voucher");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            log.error("Unexpected error processing payment for invoice {} at {}: {}", invoiceId, LocalDateTime.now(),
                    e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Thanh toán thất bại: Lỗi hệ thống.");
            response.put("failureUrl",
                    "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Lỗi hệ thống");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private Payments.PaymentMethod determinePaymentMethod(String paymentMethod, String wallet) {
        if (paymentMethod != null) {
            try {
                return Payments.PaymentMethod.valueOf(paymentMethod.toUpperCase());
            } catch (IllegalArgumentException e) {
                if (wallet != null) {
                    return Payments.PaymentMethod.valueOf(wallet.toUpperCase());
                }
            }
        } else if (wallet != null) {
            return Payments.PaymentMethod.valueOf(wallet.toUpperCase());
        }
        return Payments.PaymentMethod.TIỀN_MẶT;
    }

    @GetMapping("/guest/success-thanhtoan")
    @Transactional(readOnly = true)
    public String viewPaymentSuccessPage(
            @RequestParam("invoiceId") String invoiceId,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            Model model,
            HttpSession session) {
        try {
            log.info("Loading payment success page with invoiceId={}, room_id={}, hostel_id={} at {}",
                    invoiceId, roomId, hostelId, LocalDateTime.now());

            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt)
                    .orElseThrow(() -> {
                        log.error("Payment not found with id: {} at {}", invoiceId, LocalDateTime.now());
                        return new IllegalArgumentException("Payment not found with id: " + invoiceId);
                    });

            Rooms room;
            if (roomId != null && hostelId != null) {
                room = roomsRepository.findById(roomId)
                        .filter(r -> r.getHostel() != null && r.getHostel().getHostelId().equals(hostelId))
                        .orElseThrow(() -> {
                            log.error("Room not found or hostel mismatch for room_id: {} at {}", roomId, LocalDateTime.now());
                            return new IllegalArgumentException(
                                    "Room not found or hostel mismatch for room_id: " + roomId);
                        });
            } else {
                Contracts contract = contractsRepository.findById(payment.getContract().getContractId())
                        .orElseThrow(() -> {
                            log.error("Contract not found for payment ID: {} at {}", invoiceId, LocalDateTime.now());
                            return new IllegalArgumentException("Contract not found for payment ID: " + invoiceId);
                        });
                room = roomsRepository.findById(contract.getRoom().getRoomId())
                        .orElseThrow(() -> {
                            log.error("Room not found for contract ID: {} at {}", contract.getContractId(), LocalDateTime.now());
                            return new IllegalArgumentException(
                                    "Room not found for contract ID: " + contract.getContractId());
                        });
            }

            // Get payment details
            List<DetailPayments> details = detailPaymentsRepository.findByPaymentId(paymentIdInt);

            // Use String address directly
            String fullAddress = room.getHostel() != null && room.getHostel().getAddress() != null
                    ? room.getHostel().getAddress()
                    : "N/A";

            // Set model attributes
            model.addAttribute("invoiceId", invoiceId);
            model.addAttribute("paymentDate", Optional.ofNullable(payment.getPaymentDate())
                    .map(date -> date.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .orElse("N/A"));
            model.addAttribute("paymentTime", Optional.ofNullable(payment.getPaymentDate())
                    .map(date -> date.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .orElse("N/A"));
            model.addAttribute("paymentMethod", getPaymentMethodDisplayName(payment.getPaymentMethod()));
            model.addAttribute("roomName", Optional.ofNullable(room.getNamerooms()).orElse("N/A"));
            model.addAttribute("month", Optional.ofNullable(payment.getDueDate())
                    .map(date -> String.format("Tháng %02d/%d", date.toLocalDate().getMonthValue(),
                            date.toLocalDate().getYear()))
                    .orElse("N/A"));
             // Set totals explicitly for clarity
            model.addAttribute("originalTotal", Optional.ofNullable(model.getAttribute("originalTotal"))
                    .map(obj -> (String) obj)
                    .orElse("0 VNĐ"));
            model.addAttribute("discountAmount", Optional.ofNullable(model.getAttribute("discountAmount"))
                    .map(obj -> (String) obj)
                    .orElse("0 VNĐ"));
            model.addAttribute("finalTotal", Optional.ofNullable(payment.getTotalAmount())
                    .map(amount -> String.format("%,d VNĐ", amount.intValue()))
                    .orElse("0 VNĐ"));
            model.addAttribute("hostelAddress", fullAddress);

            // Retrieve voucher code from session
            String voucherCode = (String) session.getAttribute("voucherCode_" + invoiceId);
            model.addAttribute("voucherCode", voucherCode != null ? voucherCode : "N/A");

            // Process payment details
            processPaymentDetails(details, room, model);

            log.info("Payment success page loaded successfully for invoiceId: {}", invoiceId);
            return "guest/success-thanhtoan";
        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {} at {}", invoiceId, LocalDateTime.now(), e);
            model.addAttribute("error", "Mã hóa đơn không hợp lệ: " + invoiceId);
            return "guest/success-thanhtoan";
        } catch (IllegalArgumentException e) {
            log.error("Entity not found for invoiceId: {} at {}", invoiceId, LocalDateTime.now(), e);
            model.addAttribute("error", e.getMessage());
            return "guest/success-thanhtoan";
        } catch (Exception e) {
            log.error("Unexpected error loading payment success page at {}: {}", LocalDateTime.now(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi hệ thống: Vui lòng thử lại sau.");
            return "guest/success-thanhtoan";
        }
    }

    @GetMapping("/guest/failure-thanhtoan")
    public String viewPaymentFailurePage(
            @RequestParam("invoiceId") String invoiceId,
            @RequestParam(value = "errorMessage", required = false) String errorMessage,
            @RequestParam(value = "errorDetails", required = false) String errorDetails,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            Model model) {
        try {
            log.info("Loading payment failure page with invoiceId={}, errorMessage={} at {}",
                    invoiceId, errorMessage, LocalDateTime.now());

            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt).orElse(null);

            if (payment != null) {
                // Get payment details for display
                Contracts contract = contractsRepository.findById(payment.getContract().getContractId()).orElse(null);
                Rooms room = null;

                if (contract != null) {
                    room = roomsRepository.findById(contract.getRoom().getRoomId()).orElse(null);
                }

                // Set payment information
                model.addAttribute("invoiceId", invoiceId);
                model.addAttribute("totalAmount", Optional.ofNullable(payment.getTotalAmount())
                        .map(amount -> String.format("%,d VNĐ", amount.intValue()))
                        .orElse("0 VNĐ"));
                model.addAttribute("month", Optional.ofNullable(payment.getDueDate())
                        .map(date -> String.format("Tháng %02d/%d", date.toLocalDate().getMonthValue(),
                                date.toLocalDate().getYear()))
                        .orElse("N/A"));

                if (room != null) {
                    model.addAttribute("roomName", room.getNamerooms());
                }
            } else {
                model.addAttribute("invoiceId", invoiceId);
            }

            // Set error information
            model.addAttribute("errorMessage", errorMessage != null ? errorMessage : "Thanh toán không thành công");
            model.addAttribute("errorDetails", errorDetails);

            log.info("Payment failure page loaded successfully for invoiceId: {}", invoiceId);
            return "guest/failure-thanhtoan";
        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {} at {}", invoiceId, LocalDateTime.now(), e);
            model.addAttribute("errorMessage", "Mã hóa đơn không hợp lệ: " + invoiceId);
            model.addAttribute("invoiceId", invoiceId);
            return "guest/failure-thanhtoan";
        } catch (Exception e) {
            log.error("Unexpected error loading payment failure page at {}: {}", LocalDateTime.now(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Lỗi hệ thống: Vui lòng thử lại sau.");
            model.addAttribute("invoiceId", invoiceId);
            return "guest/failure-thanhtoan";
        }
    }

    @GetMapping("/check-payment-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@RequestParam("invoiceId") String invoiceId) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Checking payment status for invoiceId: {}", invoiceId);
            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + invoiceId));

            response.put("success", true);
            response.put("status", payment.getPaymentStatus().toString());
            response.put("isPaid", payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
            response.put("paymentMethod",
                    payment.getPaymentMethod() != null ? payment.getPaymentMethod().toString() : null);
            response.put("paymentDate", payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : null);

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                response.put("redirectUrl", "/guest/success-thanhtoan?invoiceId=" + invoiceId);
                log.info("Payment {} is completed, redirecting to success page", invoiceId);
            } else {
                String errorMessage = "Thanh toán chưa hoàn tất";
                if (payment.getPaymentStatus() == Payments.PaymentStatus.CHƯA_THANH_TOÁN) {
                    errorMessage = "Thanh toán chưa được thực hiện";
                } else if (payment.getPaymentStatus() == Payments.PaymentStatus.QUÁ_HẠN_THANH_TOÁN) {
                    errorMessage = "Thanh toán đã quá hạn";
                }
                response.put("failureUrl",
                        "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=" + errorMessage);
                log.info("Payment {} is not completed, status: {}", invoiceId, payment.getPaymentStatus());
            }

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {} at {}", invoiceId, LocalDateTime.now(), e);
            response.put("success", false);
            response.put("error", "Mã hóa đơn không hợp lệ");
            response.put("failureUrl",
                    "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Mã hóa đơn không hợp lệ");
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Payment not found for invoiceId: {} at {}", invoiceId, LocalDateTime.now(), e);
            response.put("success", false);
            response.put("error", "Không tìm thấy thông tin thanh toán");
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId
                    + "&errorMessage=Không tìm thấy thông tin thanh toán");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error checking payment status for invoiceId: {} at {}", invoiceId, LocalDateTime.now(), e);
            response.put("success", false);
            response.put("error", "Không thể kiểm tra trạng thái thanh toán");
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId
                    + "&errorMessage=Không thể kiểm tra trạng thái thanh toán");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String getPaymentMethodDisplayName(Payments.PaymentMethod method) {
        if (method == null)
            return "N/A";
        switch (method) {
            case VNPAY:
                return "VNPay";
            case MOMO:
                return "MoMo";
            case ZALOPAY:
                return "ZaloPay";
            case TIỀN_MẶT:
                return "Tiền mặt";
            default:
                return method.toString();
        }
    }

    private void processPaymentDetails(List<DetailPayments> details, Rooms room, Model model) {
        // Initialize accumulators
        Float originalTotal = 0f;
        Float discountAmount = 0f;
        String voucherCode = null;

        // Initialize default values
        model.addAttribute("roomPrice", Optional.ofNullable(room.getPrice())
                .map(price -> String.format("%,d VNĐ", price.intValue()))
                .orElse("0 VNĐ"));
        model.addAttribute("electricCost", "0 VNĐ");
        model.addAttribute("electricUsage", "0 kWh");
        model.addAttribute("electricReadings", "N/A");
        model.addAttribute("waterCost", "0 VNĐ");
        model.addAttribute("waterUsage", "0 m³");
        model.addAttribute("waterReadings", "N/A");
        model.addAttribute("serviceFee", "0 VNĐ");

        // Loop through all details to calculate breakdown
        for (DetailPayments detail : details) {
            try {
                String itemNameLower = detail.getItemName().toLowerCase();
                Double quantity = Optional.ofNullable(detail.getQuantity()).orElse(0).doubleValue();
                Double unitPrice = Optional.ofNullable(detail.getUnitPrice()).orElse(0f).doubleValue();
                Double amountUnitPrice = Optional.ofNullable(detail.getAmountUnitPrice()).orElse(0f).doubleValue();

                if (itemNameLower.startsWith("giảm giá voucher ")) {
                    discountAmount += Math.abs(amountUnitPrice.floatValue());
                    voucherCode = itemNameLower.substring("giảm giá voucher ".length()).trim().toUpperCase();
                } else {
                    originalTotal += amountUnitPrice.floatValue();
                    if (itemNameLower.contains("điện") || itemNameLower.contains("dien")
                            || itemNameLower.contains("electric")) {
                        model.addAttribute("electricCost", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                        model.addAttribute("electricUsage", String.format("%.1f kWh", quantity));
                        model.addAttribute("electricReadings", String.format("(%.0f → %.0f)", 0.0, quantity));
                    } else if (itemNameLower.contains("nước") || itemNameLower.contains("nuoc")
                            || itemNameLower.contains("water")) {
                        model.addAttribute("waterCost", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                        model.addAttribute("waterUsage", String.format("%.0f m³", quantity));
                        model.addAttribute("waterReadings", String.format("(%.0f → %.0f)", 0.0, quantity));
                    } else if (itemNameLower.contains("dịch vụ") || itemNameLower.contains("dich vu")
                            || itemNameLower.contains("phí") || itemNameLower.contains("phi")
                            || itemNameLower.contains("service") || itemNameLower.contains("fee")) {
                        model.addAttribute("serviceFee", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                    }
                }
            } catch (Exception e) {
                log.error("Error processing detail payment item: {} at {}", detail.getItemName(), LocalDateTime.now(), e);
            }
        }

        // Set calculated totals
        model.addAttribute("originalTotal", String.format("%,d VNĐ", originalTotal.intValue()));
        model.addAttribute("discountAmount", String.format("%,d VNĐ", discountAmount.intValue()));
        model.addAttribute("voucherCode", voucherCode != null ? voucherCode : "N/A");
    }

    @PostMapping("/apply-voucher")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> applyVoucher(@RequestBody Map<String, String> request,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Check invoiceId
            String invoiceIdStr = request.get("invoiceId");
            if (invoiceIdStr == null || invoiceIdStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Mã hóa đơn không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            Integer invoiceId;
            try {
                invoiceId = Integer.parseInt(invoiceIdStr);
            } catch (NumberFormatException e) {
                log.error("Invalid invoiceId format: {} at {}", invoiceIdStr, LocalDateTime.now(), e);
                response.put("success", false);
                response.put("error", "Mã hóa đơn không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }

            // Check payment
            Payments payment = paymentsRepository.findById(invoiceId)
                    .orElseThrow(() -> {
                        log.error("Payment not found with id: {} at {}", invoiceId, LocalDateTime.now());
                        return new IllegalArgumentException("Hóa đơn không tồn tại với mã: " + invoiceId);
                    });

            // Check if voucher already applied
            List<DetailPayments> details = detailPaymentsRepository.findByPaymentId(invoiceId);
            boolean alreadyApplied = details.stream()
                    .anyMatch(detail -> detail.getItemName().toLowerCase().contains("giảm giá voucher"));
            if (alreadyApplied) {
                response.put("success", false);
                response.put("error", "Voucher đã được áp dụng cho hóa đơn này");
                return ResponseEntity.badRequest().body(response);
            }

            // Check voucher code
            String voucherCode = request.get("voucherCode");
            if (voucherCode == null || voucherCode.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Mã voucher không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate voucher
            Vouchers voucher = voucherService.getVoucherByCode(voucherCode);
            if (voucher == null) {
                response.put("success", false);
                response.put("error", "Mã voucher không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            if (!voucher.getStatus()) {
                response.put("success", false);
                response.put("error", "Voucher không còn hiệu lực");
                return ResponseEntity.badRequest().body(response);
            }

            if (voucher.getQuantity() <= 0) {
                response.put("success", false);
                response.put("error", "Voucher đã hết số lượng");
                return ResponseEntity.badRequest().body(response);
            }

            if (voucher.getEndDate() != null && voucher.getEndDate().before(new Date(System.currentTimeMillis()))) {
                response.put("success", false);
                response.put("error", "Voucher đã hết hạn");
                return ResponseEntity.badRequest().body(response);
            }

            if (voucher.getMinAmount() != null && payment.getTotalAmount() < voucher.getMinAmount()) {
                response.put("success", false);
                response.put("error",
                        "Hóa đơn không đạt giá trị tối thiểu để sử dụng voucher (" + voucher.getMinAmount() + " VNĐ)");
                return ResponseEntity.badRequest().body(response);
            }

            // Calculate discount
            Float discount = voucher.getDiscountValue();
            if (payment.getTotalAmount() - discount < 0) {
                discount = payment.getTotalAmount().floatValue();
            }

            // Apply discount and save
            payment.setTotalAmount(payment.getTotalAmount() - discount);
            paymentsRepository.save(payment);

            // Save discount detail
            DetailPayments discountDetail = DetailPayments.builder()
                    .payment(payment)
                    .itemName("Giảm giá voucher " + voucherCode)
                    .quantity(1)
                    .unitPrice(-discount)
                    .amountUnitPrice(-discount)
                    .build();
            detailPaymentsRepository.save(discountDetail);

            // Store voucher info in session
            session.setAttribute("originalTotal_" + invoiceId, payment.getTotalAmount() + discount);
            session.setAttribute("discountAmount_" + invoiceId, discount);
            session.setAttribute("voucherCode_" + invoiceId, voucherCode);

            response.put("success", true);
            response.put("discountValue", discount);
            response.put("finalTotal", payment.getTotalAmount());
            response.put("message", "Áp dụng voucher thành công! ");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error in apply-voucher for invoiceId: {} at {}", request.get("invoiceId"),
                    LocalDateTime.now(), e);
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: Vui lòng thử lại sau");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/nap-rut")
    public String rutTien() {
        return "redirect:/staff/transactions/duyet-nap-rut";
    }

    @PostMapping("/landlord/confirm-cash-payment")
    @Transactional
    public ResponseEntity<Map<String, Object>> confirmCashPayment(
            @RequestParam("paymentId") Integer paymentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Payments payment = paymentsRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + paymentId));

            if (payment.getPaymentStatus() != Payments.PaymentStatus.CHỜ_XÁC_NHẬN_TIỀN_MẶT) {
                response.put("success", false);
                response.put("message", "Payment is not in waiting confirmation status.");
                return ResponseEntity.badRequest().body(response);
            }

            // Update payment status and set payment method to cash
            payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
            payment.setPaymentMethod(Payments.PaymentMethod.TIỀN_MẶT);
            payment.setPaymentDate(new java.sql.Timestamp(System.currentTimeMillis()));
            paymentsRepository.save(payment);

            // Send success notification email to tenant
            Contracts contract = payment.getContract();
            if (contract != null && contract.getTenant() != null) {
                String tenantEmail = contract.getTenant().getEmail();
                String tenantName = contract.getTenant().getFullname();
                String roomName = contract.getRoom().getNamerooms();
                String hostelName = contract.getRoom().getHostel().getName();
                String amount = CURRENCY_FORMAT.format(payment.getTotalAmount()) + " VNĐ";
                String paymentDateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                try {
                    emailService.sendCashPaymentSuccessEmail(
                            tenantEmail, tenantName, roomName, hostelName, amount, paymentDateStr, payment.getId());
                    log.info("Sent cash payment success email to tenant {}", tenantEmail);
                } catch (Exception e) {
                    log.error("Failed to send cash payment success email: {}", e.getMessage());
                }

                // Create cash payment success notification for tenant
                try {
                    notificationService.createCashPaymentSuccessNotification(contract.getTenant(), payment);
                    log.info("Created cash payment success notification for tenant {}", contract.getTenant().getUserId());
                } catch (Exception e) {
                    log.error("Failed to create cash payment success notification: {}", e.getMessage());
                }
            }

            response.put("success", true);
            response.put("message", "Payment confirmed successfully.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to confirm payment.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to decrease voucher quantity after successful payment
     */
    private void decreaseVoucherQuantity(Integer paymentId, String invoiceId) {
        try {
            List<DetailPayments> detailsAfter = detailPaymentsRepository.findByPaymentId(paymentId);
            for (DetailPayments detail : detailsAfter) {
                String itemName = detail.getItemName().toLowerCase();
                if (itemName.startsWith("giảm giá voucher ")) {
                    String usedVoucherCode = itemName.substring("giảm giá voucher ".length()).trim().toUpperCase();
                    Vouchers usedVoucher = voucherService.getVoucherByCode(usedVoucherCode);
                    if (usedVoucher != null) {
                        int newQuantity = usedVoucher.getQuantity() - 1;
                        log.info("Decreasing voucher {} quantity from {} to {} for payment {}",
                                usedVoucherCode, usedVoucher.getQuantity(), newQuantity, invoiceId);
                        usedVoucher.setQuantity(newQuantity);
                        if (newQuantity <= 0) {
                            usedVoucher.setStatus(false);
                            log.info("Voucher {} quantity reached 0, setting status to inactive", usedVoucherCode);
                        }
                        voucherRepository.save(usedVoucher);
                        log.info("Successfully updated voucher {} quantity to {} for payment {}",
                                usedVoucherCode, newQuantity, invoiceId);
                    } else {
                        log.warn("Voucher with code {} not found for payment {}", usedVoucherCode, invoiceId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error decreasing voucher quantity for payment {}: {}", invoiceId, e.getMessage(), e);
        }
    }
}
