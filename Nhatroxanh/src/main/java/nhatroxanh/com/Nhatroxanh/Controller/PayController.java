package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.DetailPayments;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Ward;
import nhatroxanh.com.Nhatroxanh.Model.enity.District;
import nhatroxanh.com.Nhatroxanh.Repository.AddressRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.DetailPaymentsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.PaymentsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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

    @GetMapping("/thanh-toan")
    @Transactional(readOnly = true)
    public String viewPaymentPage(
            @RequestParam("invoiceId") String invoiceId,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
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

            Contracts contract;
            Rooms room;

            // Priority: Use provided room_id, hostel_id, and address_id if available
            if (roomId != null && hostelId != null && addressId != null) {
                room = roomsRepository.findById(roomId)
                        .filter(r -> r.getHostel() != null && r.getHostel().getHostelId().equals(hostelId))
                        .orElseThrow(() -> new IllegalArgumentException("Room not found or hostel mismatch for room_id: " + roomId));
                Address hostelAddress = addressRepository.findById(addressId)
                        .orElseThrow(() -> new IllegalArgumentException("Address not found for address_id: " + addressId));
                if (room.getHostel().getAddress() == null) {
                    room.getHostel().setAddress(hostelAddress);
                } else if (!room.getHostel().getAddress().getId().equals(addressId)) {
                    log.warn("Provided address_id {} does not match room's address_id {} at {}", 
                            addressId, room.getHostel().getAddress().getId(), LocalDateTime.now());
                }
            } else {
                // Fallback: Use invoiceId to derive room
                contract = contractsRepository.findById(payment.getContract().getContractId())
                        .orElseThrow(() -> {
                            log.error("Contract not found for payment ID: {} at {}", invoiceId, LocalDateTime.now());
                            return new IllegalArgumentException("Contract not found for payment ID: " + invoiceId);
                        });
                room = roomsRepository.findByIdWithFullAddress(contract.getRoom().getRoomId())
                        .orElseThrow(() -> {
                            log.error("Room not found for contract ID: {} at {}", contract.getContractId(), LocalDateTime.now());
                            return new IllegalArgumentException("Room not found for contract ID: " + contract.getContractId());
                        });
            }

            // Build full address
            Address hostelAddress = room.getHostel() != null ? room.getHostel().getAddress() : null;
            String fullAddress = buildFullAddress(hostelAddress);

            model.addAttribute("invoiceId", invoiceId);
            model.addAttribute("totalAmount", Optional.ofNullable(payment.getTotalAmount())
                .map(amount -> String.format("%,d VNĐ", amount.intValue()))
                .orElse("0 VNĐ"));
            model.addAttribute("roomPrice", Optional.ofNullable(room.getPrice())
                .map(price -> String.format("%,d VNĐ", price.intValue()))
                .orElse("0 VNĐ"));
            model.addAttribute("roomName", Optional.ofNullable(room).map(Rooms::getNamerooms).orElse("Không xác định"));
            model.addAttribute("hostName", Optional.ofNullable(room)
                .map(Rooms::getHostel)
                .map(hostel -> hostel.getOwner())
                .map(owner -> owner.getFullname())
                .orElse("Không xác định"));
            model.addAttribute("dueDate", Optional.ofNullable(payment.getDueDate()).map(Object::toString).orElse("Không xác định"));
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
            } else if (paymentStatus == Payments.PaymentStatus.CHƯA_THANH_TOÁN) {
                status = "PENDING";
            } else {
                status = "PENDING";
            }
            model.addAttribute("status", status);

            model.addAttribute("paymentMethod", Optional.ofNullable(payment.getPaymentMethod()).map(Object::toString).orElse(""));
            model.addAttribute("paymentDate", Optional.ofNullable(payment.getPaymentDate()).map(Object::toString).orElse(null));

            // Initialize utility defaults
            List<DetailPayments> details = detailPaymentsRepository.findByPaymentId(paymentIdInt);
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

                    if (itemNameLower.contains("điện") || itemNameLower.contains("dien") || itemNameLower.contains("electric")) {
                        model.addAttribute("electricUsage", String.format("%.0f kWh", quantity));
                        model.addAttribute("electricCost", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                        model.addAttribute("electricUnitPrice", String.format("%,d VNĐ", unitPrice.intValue()));
                        Double prevElectric = 0.0;
                        model.addAttribute("prevElectricReading", String.format("%.0f", prevElectric));
                        model.addAttribute("currElectricReading", String.format("%.0f", prevElectric + quantity));
                    } else if (itemNameLower.contains("nước") || itemNameLower.contains("nuoc") || itemNameLower.contains("water")) {
                        model.addAttribute("waterUsage", String.format("%.0f m³", quantity));
                        model.addAttribute("waterCost", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                        model.addAttribute("waterUnitPrice", String.format("%,d VNĐ", unitPrice.intValue()));
                        Double prevWater = 0.0;
                        model.addAttribute("prevWaterReading", String.format("%.0f", prevWater));
                        model.addAttribute("currWaterReading", String.format("%.0f", prevWater + quantity));
                    } else if (itemNameLower.contains("dịch vụ") || itemNameLower.contains("dich vu") || itemNameLower.contains("phí") || itemNameLower.contains("phi") || itemNameLower.contains("service") || itemNameLower.contains("fee")) {
                        model.addAttribute("serviceFee", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                    }
                } catch (Exception e) {
                    log.error("Error processing detail payment item: {} at {}", detail.getItemName(), LocalDateTime.now(), e);
                }
            });

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
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestParam("invoiceId") String invoiceId,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "wallet", required = false) String wallet,
            @RequestParam(value = "paymentDate", required = false) String paymentDate,
            @RequestParam(value = "paymentTime", required = false) String paymentTime,
            @RequestParam(value = "paymentNote", required = false) String paymentNote) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Processing payment with invoiceId={}, room_id={}, hostel_id={}, address_id={}, method={} at {}", 
                    invoiceId, roomId, hostelId, addressId, paymentMethod, LocalDateTime.now());
            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + invoiceId));

            if (payment.getPaymentStatus() == Payments.PaymentStatus.ĐÃ_THANH_TOÁN) {
                throw new IllegalStateException("Hóa đơn đã được thanh toán.");
            }

            payment.setPaymentStatus(Payments.PaymentStatus.ĐÃ_THANH_TOÁN);
            payment.setPaymentDate(new java.sql.Date(System.currentTimeMillis()));
            Payments.PaymentMethod methodEnum;
            if (paymentMethod != null) {
                try {
                    methodEnum = Payments.PaymentMethod.valueOf(paymentMethod.toUpperCase());
                } catch (IllegalArgumentException e) {
                    if (wallet != null) {
                        methodEnum = Payments.PaymentMethod.valueOf(wallet.toUpperCase());
                    } else {
                        methodEnum = Payments.PaymentMethod.TIỀN_MẶT;
                    }
                }
            } else if (wallet != null) {
                methodEnum = Payments.PaymentMethod.valueOf(wallet.toUpperCase());
            } else {
                methodEnum = Payments.PaymentMethod.TIỀN_MẶT;
            }
            payment.setPaymentMethod(methodEnum);

            if ("cash".equalsIgnoreCase(paymentMethod) && paymentDate != null && paymentTime != null) {
                log.info("Scheduled cash payment for invoice {} on {} at {}", invoiceId, paymentDate, paymentTime);
            }

            paymentsRepository.save(payment);

            response.put("success", true);
            response.put("message", "Thanh toán thành công!");
            response.put("redirectUrl", "/guest/success-thanhtoan?invoiceId=" + invoiceId);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {} at {}", invoiceId, LocalDateTime.now(), e);
            response.put("success", false);
            response.put("error", "Mã hóa đơn không hợp lệ.");
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Mã hóa đơn không hợp lệ");
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error processing payment for invoice {} at {}: {}", invoiceId, LocalDateTime.now(), e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error processing payment for invoice {} at {}: {}", invoiceId, LocalDateTime.now(), e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Thanh toán thất bại: Lỗi hệ thống.");
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Lỗi hệ thống");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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
                        .orElseThrow(() -> new IllegalArgumentException("Room not found or hostel mismatch for room_id: " + roomId));
                Address hostelAddress = addressRepository.findById(addressId)
                        .orElseThrow(() -> new IllegalArgumentException("Address not found for address_id: " + addressId));
                if (room.getHostel().getAddress() == null) {
                    room.getHostel().setAddress(hostelAddress);
                } else if (!room.getHostel().getAddress().getId().equals(addressId)) {
                    log.warn("Provided address_id {} does not match room's address_id {} at {}", 
                            addressId, room.getHostel().getAddress().getId(), LocalDateTime.now());
                }
            } else {
                // Fallback: Use invoiceId to derive room
                contract = contractsRepository.findById(payment.getContract().getContractId())
                        .orElseThrow(() -> {
                            log.error("Contract not found for payment ID: {} at {}", invoiceId, LocalDateTime.now());
                            return new IllegalArgumentException("Contract not found for payment ID: " + invoiceId);
                        });
                room = roomsRepository.findByIdWithFullAddress(contract.getRoom().getRoomId())
                        .orElseThrow(() -> {
                            log.error("Room not found for contract ID: {} at {}", contract.getContractId(), LocalDateTime.now());
                            return new IllegalArgumentException("Room not found for contract ID: " + contract.getContractId());
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
                .map(date -> date.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .orElse("N/A"));
            model.addAttribute("paymentTime", Optional.ofNullable(payment.getPaymentDate())
                .map(date -> "10:30 AM")
                .orElse("N/A"));
            model.addAttribute("paymentMethod", getPaymentMethodDisplayName(payment.getPaymentMethod()));
            model.addAttribute("roomName", Optional.ofNullable(room.getNamerooms()).orElse("N/A"));
            model.addAttribute("month", Optional.ofNullable(payment.getDueDate())
                .map(date -> String.format("Tháng %02d/%d", date.toLocalDate().getMonthValue(), date.toLocalDate().getYear()))
                .orElse("N/A"));
            model.addAttribute("totalAmount", Optional.ofNullable(payment.getTotalAmount())
                .map(amount -> String.format("%,d VNĐ", amount.intValue()))
                .orElse("0 VNĐ"));
            model.addAttribute("hostelAddress", fullAddress);

            // Process payment details for breakdown
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
                    room = roomsRepository.findByIdWithFullAddress(contract.getRoom().getRoomId()).orElse(null);
                }
                
                // Set payment information
                model.addAttribute("invoiceId", invoiceId);
                model.addAttribute("totalAmount", Optional.ofNullable(payment.getTotalAmount())
                    .map(amount -> String.format("%,d VNĐ", amount.intValue()))
                    .orElse("0 VNĐ"));
                model.addAttribute("month", Optional.ofNullable(payment.getDueDate())
                    .map(date -> String.format("Tháng %02d/%d", date.toLocalDate().getMonthValue(), date.toLocalDate().getYear()))
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
            response.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().toString() : null);
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
                response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=" + errorMessage);
                log.info("Payment {} is not completed, status: {}", invoiceId, payment.getPaymentStatus());
            }
            
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {}", invoiceId, e);
            response.put("success", false);
            response.put("error", "Mã hóa đơn không hợp lệ");
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Mã hóa đơn không hợp lệ");
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Payment not found for invoiceId: {}", invoiceId, e);
            response.put("success", false);
            response.put("error", "Không tìm thấy thông tin thanh toán");
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Không tìm thấy thông tin thanh toán");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error checking payment status for invoiceId: {}", invoiceId, e);
            response.put("success", false);
            response.put("error", "Không thể kiểm tra trạng thái thanh toán");
            response.put("failureUrl", "/guest/failure-thanhtoan?invoiceId=" + invoiceId + "&errorMessage=Không thể kiểm tra trạng thái thanh toán");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String buildFullAddress(Address hostelAddress) {
        if (hostelAddress == null) return "Không xác định";
        
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
        if (method == null) return "N/A";
        switch (method) {
            case VNPAY: return "VNPay";
            case MOMO: return "MoMo";
            case BANK: return "Ngân hàng";
            case TIỀN_MẶT: return "Tiền mặt";
            default: return method.toString();
        }
    }

    private void processPaymentDetails(List<DetailPayments> details, Rooms room, Model model) {
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

        // Process each detail payment
        for (DetailPayments detail : details) {
            try {
                String itemNameLower = detail.getItemName().toLowerCase();
                Double quantity = Optional.ofNullable(detail.getQuantity()).orElse(0).doubleValue();
                Double amountUnitPrice = Optional.ofNullable(detail.getAmountUnitPrice()).orElse(0f).doubleValue();

                if (itemNameLower.contains("điện") || itemNameLower.contains("dien") || itemNameLower.contains("electric")) {
                    model.addAttribute("electricCost", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                    model.addAttribute("electricUsage", String.format("%.1f kWh", quantity));
                    model.addAttribute("electricReadings", String.format("(%.0f → %.0f)", 0.0, quantity));
                } else if (itemNameLower.contains("nước") || itemNameLower.contains("nuoc") || itemNameLower.contains("water")) {
                    model.addAttribute("waterCost", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                    model.addAttribute("waterUsage", String.format("%.0f m³", quantity));
                    model.addAttribute("waterReadings", String.format("(%.0f → %.0f)", 0.0, quantity));
                } else if (itemNameLower.contains("dịch vụ") || itemNameLower.contains("dich vu") || 
                          itemNameLower.contains("phí") || itemNameLower.contains("phi") || 
                          itemNameLower.contains("service") || itemNameLower.contains("fee")) {
                    model.addAttribute("serviceFee", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                }
            } catch (Exception e) {
                log.error("Error processing detail payment item: {} at {}", detail.getItemName(), LocalDateTime.now(), e);
            }
        }
    }

     @GetMapping("/nap-rut")
    public String rutTien() {
        return "redirect:/staff/transactions/duyet-nap-rut";
    }
}
