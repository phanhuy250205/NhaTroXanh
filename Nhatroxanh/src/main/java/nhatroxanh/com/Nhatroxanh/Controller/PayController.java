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
    @Transactional(readOnly = true) // Optimize for GET requests
    public String viewPaymentPage(
            @RequestParam("invoiceId") String invoiceId,
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "hostel_id", required = false) Integer hostelId,
            @RequestParam(value = "address_id", required = false) Integer addressId,
            Model model) {
        try {
            log.info("Loading payment page with invoiceId={}, room_id={}, hostel_id={}, address_id={} at {} (04:08 PM +07, July 14, 2025)", 
                    invoiceId, roomId, hostelId, addressId, LocalDateTime.now());
            
            Integer paymentIdInt = Integer.parseInt(invoiceId);
            Payments payment = paymentsRepository.findById(paymentIdInt)
                    .orElseThrow(() -> {
                        log.error("Payment not found with id: {} at {} (04:08 PM +07, July 14, 2025)", invoiceId, LocalDateTime.now());
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
                    room.getHostel().setAddress(hostelAddress); // Manually set if provided
                } else if (!room.getHostel().getAddress().getId().equals(addressId)) {
                    log.warn("Provided address_id {} does not match room's address_id {} at {} (04:08 PM +07, July 14, 2025)", 
                            addressId, room.getHostel().getAddress().getId(), LocalDateTime.now());
                }
            } else {
                // Fallback: Use invoiceId to derive room
                contract = contractsRepository.findById(payment.getContract().getContractId())
                        .orElseThrow(() -> {
                            log.error("Contract not found for payment ID: {} at {} (04:08 PM +07, July 14, 2025)", invoiceId, LocalDateTime.now());
                            return new IllegalArgumentException("Contract not found for payment ID: " + invoiceId);
                        });
                room = roomsRepository.findByIdWithFullAddress(contract.getRoom().getRoomId())
                        .orElseThrow(() -> {
                            log.error("Room not found for contract ID: {} at {} (04:08 PM +07, July 14, 2025)", contract.getContractId(), LocalDateTime.now());
                            return new IllegalArgumentException("Room not found for contract ID: " + contract.getContractId());
                        });
            }

            // Debug hostel and address
            Address hostelAddress = room.getHostel() != null ? room.getHostel().getAddress() : null;
            if (hostelAddress == null) {
                log.warn("Hostel address not found for room ID: {} at {} (04:08 PM +07, July 14, 2025)", room.getRoomId(), LocalDateTime.now());
            } else {
                log.debug("Address details: id={}, street={}, wardId={}", 
                    hostelAddress.getId(),
                    hostelAddress.getStreet(), 
                    hostelAddress.getWard() != null ? hostelAddress.getWard().getId() : "null");
                if (hostelAddress.getWard() != null) {
                    log.debug("Ward details: id={}, name={}, districtId={}", 
                        hostelAddress.getWard().getId(),
                        hostelAddress.getWard().getName(), 
                        hostelAddress.getWard().getDistrict() != null ? hostelAddress.getWard().getDistrict().getId() : "null");
                    if (hostelAddress.getWard().getDistrict() != null) {
                        log.debug("District details: id={}, name={}, provinceId={}", 
                            hostelAddress.getWard().getDistrict().getId(),
                            hostelAddress.getWard().getDistrict().getName(), 
                            hostelAddress.getWard().getDistrict().getProvince() != null ? hostelAddress.getWard().getDistrict().getProvince().getId() : "null");
                        if (hostelAddress.getWard().getDistrict().getProvince() != null) {
                            log.debug("Province details: id={}, name={}", 
                                hostelAddress.getWard().getDistrict().getProvince().getId(),
                                hostelAddress.getWard().getDistrict().getProvince().getName());
                        }
                    }
                }
            }

            // Construct full address based on roomId
            model.addAttribute("hostelAddress", Optional.ofNullable(hostelAddress)
                .map(address -> {
                    StringBuilder fullAddress = new StringBuilder();
                    if (address.getStreet() != null) {
                        fullAddress.append(address.getStreet());
                    } else {
                        log.debug("Street is null for address ID: {} at {} (04:08 PM +07, July 14, 2025)", address.getId(), LocalDateTime.now());
                    }
                    if (address.getWard() != null) {
                        Ward ward = address.getWard();
                        if (ward.getName() != null) {
                            fullAddress.append(fullAddress.length() > 0 ? ", " : "").append(ward.getName());
                        } else {
                            log.debug("Ward name is null for ward ID: {} at {} (04:08 PM +07, July 14, 2025)", ward.getId(), LocalDateTime.now());
                        }
                        if (ward.getDistrict() != null) {
                            District district = ward.getDistrict();
                            if (district.getName() != null) {
                                fullAddress.append(fullAddress.length() > 0 ? ", " : "").append(district.getName());
                            } else {
                                log.debug("District name is null for district ID: {} at {} (04:08 PM +07, July 14, 2025)", district.getId(), LocalDateTime.now());
                            }
                            if (district.getProvince() != null && district.getProvince().getName() != null) {
                                fullAddress.append(fullAddress.length() > 0 ? ", " : "").append(district.getProvince().getName());
                            } else {
                                log.debug("Province or province name is null for district ID: {} at {} (04:08 PM +07, July 14, 2025)", district.getId(), LocalDateTime.now());
                            }
                        } else {
                            log.debug("District is null for ward ID: {} at {} (04:08 PM +07, July 14, 2025)", ward.getId(), LocalDateTime.now());
                        }
                    } else {
                        log.debug("Ward is null for address ID: {} at {} (04:08 PM +07, July 14, 2025)", address.getId(), LocalDateTime.now());
                    }
                    return fullAddress.length() > 0 ? fullAddress.toString() : "Không xác định";
                })
                .orElse("Không xác định"));

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
                status = "PENDING"; // Default fallback
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
                        Double prevElectric = 0.0; // Placeholder: Implement actual fetch
                        model.addAttribute("prevElectricReading", String.format("%.0f", prevElectric));
                        model.addAttribute("currElectricReading", String.format("%.0f", prevElectric + quantity));
                    } else if (itemNameLower.contains("nước") || itemNameLower.contains("nuoc") || itemNameLower.contains("water")) {
                        model.addAttribute("waterUsage", String.format("%.0f m³", quantity));
                        model.addAttribute("waterCost", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                        model.addAttribute("waterUnitPrice", String.format("%,d VNĐ", unitPrice.intValue()));
                        Double prevWater = 0.0; // Placeholder: Implement actual fetch
                        model.addAttribute("prevWaterReading", String.format("%.0f", prevWater));
                        model.addAttribute("currWaterReading", String.format("%.0f", prevWater + quantity));
                    } else if (itemNameLower.contains("dịch vụ") || itemNameLower.contains("dich vu") || itemNameLower.contains("phí") || itemNameLower.contains("phi") || itemNameLower.contains("service") || itemNameLower.contains("fee")) {
                        model.addAttribute("serviceFee", String.format("%,d VNĐ", amountUnitPrice.intValue()));
                    }
                } catch (Exception e) {
                    log.error("Error processing detail payment item: {} at {} (04:08 PM +07, July 14, 2025)", detail.getItemName(), LocalDateTime.now(), e);
                }
            });

            log.debug("Model attributes for invoiceId {} at {} (04:08 PM +07, July 14, 2025): {}", invoiceId, LocalDateTime.now(), model.asMap());
            return "guest/thanh-toan";
        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {} at {} (04:08 PM +07, July 14, 2025)", invoiceId, LocalDateTime.now(), e);
            model.addAttribute("error", "Mã hóa đơn không hợp lệ: " + invoiceId);
            return "guest/thanh-toan";
        } catch (IllegalArgumentException e) {
            log.error("Entity not found for invoiceId: {} at {} (04:08 PM +07, July 14, 2025)", invoiceId, LocalDateTime.now(), e);
            model.addAttribute("error", e.getMessage());
            return "guest/thanh-toan";
        } catch (Exception e) {
            log.error("Unexpected error loading payment page at {} (04:08 PM +07, July 14, 2025): {}", LocalDateTime.now(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi hệ thống: Vui lòng thử lại sau. Chi tiết: " + e.getMessage());
            return "guest/thanh-toan"; // Ensure a response is returned
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
            log.info("Processing payment with invoiceId={}, room_id={}, hostel_id={}, address_id={}, method={} at {} (04:08 PM +07, July 14, 2025)", 
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
                log.info("Scheduled cash payment for invoice {} on {} at {} (04:08 PM +07, July 14, 2025)", invoiceId, paymentDate, paymentTime);
                // Additional logic for scheduling if needed
            }

            paymentsRepository.save(payment);

            response.put("success", true);
            response.put("message", "Thanh toán thành công!");
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Invalid invoiceId format: {} at {} (04:08 PM +07, July 14, 2025)", invoiceId, LocalDateTime.now(), e);
            response.put("success", false);
            response.put("error", "Mã hóa đơn không hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error processing payment for invoice {} at {} (04:08 PM +07, July 14, 2025): {}", invoiceId, LocalDateTime.now(), e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error processing payment for invoice {} at {} (04:08 PM +07, July 14, 2025): {}", invoiceId, LocalDateTime.now(), e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Thanh toán thất bại: Lỗi hệ thống.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}