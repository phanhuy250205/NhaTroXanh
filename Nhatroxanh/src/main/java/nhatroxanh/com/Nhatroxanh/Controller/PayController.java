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

import jakarta.servlet.http.HttpSession;

import java.sql.Date;
import java.time.LocalDateTime;
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

    @GetMapping("/thanh-toan")
    @Transactional
    public String viewPaymentPage(
            @RequestParam("invoiceId") String invoiceId,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            HttpSession session,
            Model model) {
        try {
            log.info("Loading payment page with invoiceId={}, room_id={}, hostel_id={}, address_id={} at {}",
                    invoiceId, roomId, hostelId, addressId, LocalDateTime.now());

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

            Contracts contract;
            Rooms room;

            // Priority: Use provided room_id, hostel_id, and address_id if available
            if (roomId != null && hostelId != null && addressId != null) {
                room = roomsRepository.findById(roomId)
                        .filter(r -> r.getHostel() != null && r.getHostel().getHostelId().equals(hostelId))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Room not found or hostel mismatch for room_id: " + roomId));
                Address hostelAddress = addressRepository.findById(addressId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Address not found for address_id: " + addressId));
                if (room.getHostel().getAddress() == null) {
                    room.getHostel().setAddress(hostelAddress);
                } else if (!room.getHostel().getAddress().getId().equals(addressId)) {
                    log.warn("Provided address_id {} does not match room's address_id {} at {}",
                            addressId, room.getHostel().getAddress().getId(), LocalDateTime.now());
                }
            } else {
                contract = contractsRepository.findById(payment.getContract().getContractId())
                        .orElseThrow(() -> {
                            log.error("Contract not found for payment ID: {} at {}", invoiceId, LocalDateTime.now());
                            return new IllegalArgumentException("Contract not found for payment ID: " + invoiceId);
                        });
                room = roomsRepository.findByIdWithFullAddress(contract.getRoom().getRoomId())
                        .orElseThrow(() -> {
                            log.error("Room not found for contract ID: {} at {}", contract.getContractId(),
                                    LocalDateTime.now());
                            return new IllegalArgumentException(
                                    "Room not found for contract ID: " + contract.getContractId());
                        });
            }

            // Build full address
            Address hostelAddress = room.getHostel() != null ? room.getHostel().getAddress() : null;
            String fullAddress = buildFullAddress(hostelAddress);

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
            model.addAttribute("dueDate",
                    Optional.ofNullable(payment.getDueDate()).map(Object::toString).orElse("Không xác định"));
            model.addAttribute("month", Optional.ofNullable(payment.getDueDate())
                    .map(date -> date.toLocalDate().getMonthValue() + "/" + date.toLocalDate().getYear())
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
                    Optional.ofNullable(payment.getPaymentMethod()).map(this::getPaymentMethodDisplayName).orElse(""));
            model.addAttribute("paymentDate",
                    Optional.ofNullable(payment.getPaymentDate())
                            .map(timestamp -> timestamp.toLocalDateTime()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                            .orElse("N/A"));

            // Initialize utility defaults
            processPaymentDetails(details, room, model);

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
            log.info("Processing payment with invoiceId={}, voucherCode={} at {}", invoiceId, voucherCode,
                    LocalDateTime.now());
            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt)
                    .orElseThrow(() -> new IllegalArgumentException("Hóa đơn không tồn tại với mã: " + invoiceId));

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                throw new IllegalStateException("Hóa đơn đã được thanh toán.");
            }

            // Apply voucher if provided
            Float discount = 0f;
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

                        log.info(
                                "Applied voucher {} with discount {} for payment {} during processPayment",
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

            // Update payment status
            payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
            payment.setPaymentDate(new java.sql.Timestamp(System.currentTimeMillis()));
            Payments.PaymentMethod methodEnum = determinePaymentMethod(paymentMethod, wallet);
            payment.setPaymentMethod(methodEnum);

            if ("cash".equalsIgnoreCase(paymentMethod) && paymentDate != null && paymentTime != null) {
                log.info("Scheduled cash payment for invoice {} on {} at {}", invoiceId, paymentDate, paymentTime);
            }

            paymentsRepository.save(payment);

            // Decrease voucher quantity after successful payment
            List<DetailPayments> detailsAfter = detailPaymentsRepository.findByPaymentId(paymentIdInt);
            for (DetailPayments detail : detailsAfter) {
                String itemName = detail.getItemName().toLowerCase();
                if (itemName.startsWith("giảm giá voucher ")) {
                    String usedVoucherCode = itemName.substring("giảm giá voucher ".length()).trim();
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
                        voucherRepository.save(usedVoucher); // Directly save to ensure transaction commits
                        log.info("Successfully updated voucher {} quantity to {} for payment {}",
                                usedVoucherCode, newQuantity, invoiceId);
                    } else {
                        log.warn("Voucher with code {} not found for payment {}", usedVoucherCode, invoiceId);
                    }
                }
            }

            response.put("success", true);
            response.put("message", "Thanh toán thành công! Giảm giá: " + discount + " VNĐ");
            response.put("redirectUrl", "/guest/success-thanhtoan?invoiceId=" + invoiceId);
            return ResponseEntity.ok(response);

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
            @RequestParam(value = "address_id", required = false) Integer addressId,
            Model model) {
        try {
            log.info("Loading payment success page with invoiceId={}, room_id={}, hostel_id={}, address_id={} at {}",
                    invoiceId, roomId, hostelId, addressId, LocalDateTime.now());

            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt)
                    .orElseThrow(() -> {
                        log.error("Payment not found with id: {} at {}", invoiceId, LocalDateTime.now());
                        return new IllegalArgumentException("Payment not found with id: " + invoiceId);
                    });

            Contracts contract;
            Rooms room;

            // Priority: Use provided room_id, hostel_id, and address_id if available
            if (roomId != null && hostelId != null && addressId != null) {
                room = roomsRepository.findById(roomId)
                        .filter(r -> r.getHostel() != null && r.getHostel().getHostelId().equals(hostelId))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Room not found or hostel mismatch for room_id: " + roomId));
                Address hostelAddress = addressRepository.findById(addressId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Address not found for address_id: " + addressId));
                if (room.getHostel().getAddress() == null) {
                    room.getHostel().setAddress(hostelAddress);
                } else if (!room.getHostel().getAddress().getId().equals(addressId)) {
                    log.warn("Provided address_id {} does not match room's address_id {} at {}",
                            addressId, room.getHostel().getAddress().getId(), LocalDateTime.now());
                }
            } else {
                contract = contractsRepository.findById(payment.getContract().getContractId())
                        .orElseThrow(() -> {
                            log.error("Contract not found for payment ID: {} at {}", invoiceId, LocalDateTime.now());
                            return new IllegalArgumentException("Contract not found for payment ID: " + invoiceId);
                        });
                room = roomsRepository.findByIdWithFullAddress(contract.getRoom().getRoomId())
                        .orElseThrow(() -> {
                            log.error("Room not found for contract ID: {} at {}", contract.getContractId(),
                                    LocalDateTime.now());
                            return new IllegalArgumentException(
                                    "Room not found for contract ID: " + contract.getContractId());
                        });
            }

            // Get payment details
            List<DetailPayments> details = detailPaymentsRepository.findByPaymentId(paymentIdInt);

            // Build full address
            Address hostelAddress = room.getHostel() != null ? room.getHostel().getAddress() : null;
            String fullAddress = buildFullAddress(hostelAddress);

            // Set basic payment information
            model.addAttribute("invoiceId", invoiceId);
            model.addAttribute("paymentDate", Optional.ofNullable(payment.getPaymentDate())
                    .map(timestamp -> timestamp.toLocalDateTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .orElse("N/A"));
            model.addAttribute("paymentTime", Optional.ofNullable(payment.getPaymentDate())
                    .map(timestamp -> timestamp.toLocalDateTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                    .orElse("N/A"));
            model.addAttribute("paymentMethod", getPaymentMethodDisplayName(payment.getPaymentMethod()));
            model.addAttribute("roomName", Optional.ofNullable(room.getNamerooms()).orElse("N/A"));
            model.addAttribute("month", Optional.ofNullable(payment.getDueDate())
                    .map(date -> String.format("Tháng %02d/%d", date.toLocalDate().getMonthValue(),
                            date.toLocalDate().getYear()))
                    .orElse("N/A"));
            model.addAttribute("hostelAddress", fullAddress);

            // Process payment details for breakdown, including voucher discount
            processPaymentDetails(details, room, model);

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
            log.error("Unexpected error loading payment success page at {}: {}", LocalDateTime.now(), e.getMessage(),
                    e);
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
                    room = roomsRepository.findByIdWithFullAddress(contract.getRoom().getRoomId()).orElse(null);
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

                // Optionally process details for failure page if needed, but skipping breakdown
                // as per template
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
            log.error("Unexpected error loading payment failure page at {}: {}", LocalDateTime.now(), e.getMessage(),
                    e);
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
            log.error("Invalid invoiceId format: {}", invoiceId, e);
            response.put("success", false);
            response.put("error", "Mã hóa đơn không hợp lệ");
            response.put("failureUrl",
                    "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Mã hóa đơn không hợp lệ");
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Payment not found for invoiceId: {}", invoiceId, e);
            response.put("success", false);
            response.put("error", "Không tìm thấy thông tin thanh toán");
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId
                    + "&errorMessage=Không tìm thấy thông tin thanh toán");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error checking payment status for invoiceId: {}", invoiceId, e);
            response.put("success", false);
            response.put("error", "Không thể kiểm tra trạng thái thanh toán");
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId
                    + "&errorMessage=Không thể kiểm tra trạng thái thanh toán");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String buildFullAddress(Address hostelAddress) {
        if (hostelAddress == null)
            return "Không xác định";

        StringBuilder fullAddress = new StringBuilder();
        if (hostelAddress.getStreet() != null) {
            fullAddress.append(hostelAddress.getStreet());
        }
        if (hostelAddress.getWard() != null) {
            Ward ward = hostelAddress.getWard();
            if (ward.getName() != null) {
                fullAddress.append(fullAddress.length() > 0 ? ", " : "").append(ward.getName());
            }
            if (ward.getDistrict() != null) {
                District district = ward.getDistrict();
                if (district.getName() != null) {
                    fullAddress.append(fullAddress.length() > 0 ? ", " : "").append(district.getName());
                }
                if (district.getProvince() != null && district.getProvince().getName() != null) {
                    fullAddress.append(fullAddress.length() > 0 ? ", " : "").append(district.getProvince().getName());
                }
            }
        }
        return fullAddress.length() > 0 ? fullAddress.toString() : "Không xác định";
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
        Float discount = 0f;
        Float serviceFee = 0f;
        String electricCost = "0 VNĐ";
        String electricUsage = "0 kWh";
        String electricReadings = "N/A";
        String waterCost = "0 VNĐ";
        String waterUsage = "0 m³";
        String waterReadings = "N/A";
        String roomPrice = Optional.ofNullable(room.getPrice())
                .map(price -> String.format("%,d VNĐ", price.intValue()))
                .orElse("0 VNĐ");

        // Loop through all details to calculate breakdown
        for (DetailPayments detail : details) {
            try {
                String itemNameLower = detail.getItemName().toLowerCase();
                Double quantity = Optional.ofNullable(detail.getQuantity()).orElse(0).doubleValue();
                Double unitPrice = Optional.ofNullable(detail.getUnitPrice()).orElse(0f).doubleValue();
                Double amount = Optional.ofNullable(detail.getAmountUnitPrice()).orElse(0f).doubleValue();

                if (amount < 0) {
                    // Negative amounts are discounts
                    discount += Math.abs(amount.floatValue());
                } else {
                    // Positive amounts contribute to originalTotal
                    originalTotal += amount.floatValue();

                    // Categorize
                    if (itemNameLower.contains("phòng")) {
                        roomPrice = String.format("%,d VNĐ", amount.intValue());
                    } else if (itemNameLower.contains("điện") || itemNameLower.contains("dien")
                            || itemNameLower.contains("electric")) {
                        electricCost = String.format("%,d VNĐ", amount.intValue());
                        electricUsage = String.format("%.1f kWh", quantity);
                        electricReadings = String.format("(%.0f → %.0f)", 0.0, quantity);
                    } else if (itemNameLower.contains("nước") || itemNameLower.contains("nuoc")
                            || itemNameLower.contains("water")) {
                        waterCost = String.format("%,d VNĐ", amount.intValue());
                        waterUsage = String.format("%.0f m³", quantity);
                        waterReadings = String.format("(%.0f → %.0f)", 0.0, quantity);
                    } else {
                        // Other positive fees (wifi, trash, etc.) go to serviceFee
                        serviceFee += amount.floatValue();
                    }
                }
            } catch (Exception e) {
                log.error("Error processing detail payment item: {} at {}", detail.getItemName(), LocalDateTime.now(),
                        e);
            }
        }

        // Set serviceFee if any
        String serviceFeeStr = serviceFee > 0 ? String.format("%,d VNĐ", serviceFee.intValue()) : "0 VNĐ";

        // Set attributes for Thymeleaf or frontend display
        model.addAttribute("roomPrice", roomPrice);
        model.addAttribute("electricCost", electricCost);
        model.addAttribute("electricUsage", electricUsage);
        model.addAttribute("electricReadings", electricReadings);
        model.addAttribute("waterCost", waterCost);
        model.addAttribute("waterUsage", waterUsage);
        model.addAttribute("waterReadings", waterReadings);
        model.addAttribute("serviceFee", serviceFeeStr);
        model.addAttribute("originalTotal", String.format("%,d VNĐ", originalTotal.intValue()));
        model.addAttribute("discountAmount", discount > 0 ? String.format("%,d VNĐ", discount.intValue()) : "0 VNĐ");
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
                log.error("Invalid invoiceId format: {} at {}", invoiceIdStr, LocalDateTime.now());
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

            log.info(
                    "Applied voucher {} with discount {} for payment {} (quantity will be decreased on payment success)",
                    voucherCode, discount, invoiceId);

            response.put("success", true);
            response.put("discountValue", discount);
            response.put("finalTotal", payment.getTotalAmount());
            response.put("message", "Áp dụng voucher thành công! Giảm " + discount + " VNĐ");
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
}