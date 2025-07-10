package nhatroxanh.com.Nhatroxanh.Service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentRequestDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentResponseDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments.PaymentStatus;
import nhatroxanh.com.Nhatroxanh.Repository.*;
import nhatroxanh.com.Nhatroxanh.Service.PaymentService;

import java.sql.Date;
import java.time.LocalDate;
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
            // Lấy contract
            Contracts contract = contractsRepository.findById(request.getContractId())
                    .orElseThrow(() -> new RuntimeException("Contract not found with id: " + request.getContractId()));

            // Parse tháng/năm
            String[] monthYear = request.getMonth().split("/");
            int month = Integer.parseInt(monthYear[0]);
            int year = Integer.parseInt(monthYear[1]);

            // Kiểm tra xem đã có payment cho tháng này chưa
            Optional<Payments> existingPayment = paymentsRepository.findByContractIdAndMonth(
                    request.getContractId(), month, year);
            if (existingPayment.isPresent()) {
                throw new RuntimeException("Payment already exists for this month");
            }

            // Validate details
            if (request.getDetails() == null || request.getDetails().isEmpty()) {
                throw new RuntimeException("Payment details cannot be empty");
            }

            // Tính tổng tiền
            Float totalAmount = 0f;
            for (PaymentRequestDto.PaymentDetailDto detail : request.getDetails()) {
                if (detail.getAmount() == null || detail.getAmount() < 0) {
                    throw new RuntimeException("Invalid amount for item: " + detail.getItemName());
                }
                totalAmount += detail.getAmount();
            }

            // Tạo payment
            Payments payment = Payments.builder()
                    .contract(contract)
                    .totalAmount(totalAmount)
                    .dueDate(request.getDueDate())
                    .paymentStatus(PaymentStatus.CHƯA_THANH_TOÁN)
                    .paymentMethod(request.getPaymentMethod()) // Không set mặc định, giữ nguyên giá trị từ request
                    .build();

            payment = paymentsRepository.save(payment);

            // Tạo detail payments
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
            // Lấy tất cả contracts active của owner
            List<Contracts> contracts = contractRepository.findByOwnerId(ownerId);

            // Lọc chỉ những contracts đang active
            List<Contracts> activeContracts = contracts.stream()
                    .filter(contract -> contract.getStatus() == Contracts.Status.ACTIVE)
                    .collect(Collectors.toList());

            // Chuyển đổi thành format phù hợp cho frontend
            List<Map<String, Object>> result = new ArrayList<>();

            for (Contracts contract : activeContracts) {
                Map<String, Object> contractData = new HashMap<>();

                // Thông tin cơ bản
                contractData.put("contractId", contract.getContractId());
                contractData.put("roomCode", contract.getRoom().getNamerooms());
                contractData.put("hostelName", contract.getRoom().getHostel().getName());
                contractData.put("roomPrice", contract.getPrice());

                // Thông tin tenant
                String tenantName = "";
                if (contract.getTenant() != null) {
                    tenantName = contract.getTenant().getFullname();
                } else if (contract.getUnregisteredTenant() != null) {
                    tenantName = contract.getUnregisteredTenant().getFullName();
                }
                contractData.put("tenantName", tenantName);
                contractData.put("tenantPhone", contract.getTenantPhone());

                // Thêm thông tin phòng
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

    private PaymentResponseDto convertToResponseDto(Payments payment) {
        Contracts contract = payment.getContract();
        Rooms room = contract.getRoom();

        // Lấy tên tenant
        String tenantName = "";
        String tenantPhone = contract.getTenantPhone();
        if (contract.getTenant() != null) {
            tenantName = contract.getTenant().getFullname();
        } else if (contract.getUnregisteredTenant() != null) {
            tenantName = contract.getUnregisteredTenant().getFullName();
        }

        // Format tháng
        String month = "";
        if (payment.getDueDate() != null) {
            LocalDate dueDate = payment.getDueDate().toLocalDate();
            month = String.format("%02d/%d", dueDate.getMonthValue(), dueDate.getYear());
        }

        // Lấy payment details
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

    public void deletePayment(Integer paymentId) {
        try {
            Payments payment = paymentsRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

            // Delete associated detail payments first
            detailPaymentsRepository.deleteByPaymentId(paymentId);

            // Delete the payment
            paymentsRepository.delete(payment);

            log.info("Deleted payment with id: {}", paymentId);
        } catch (Exception e) {
            log.error("Error deleting payment with id {}: ", paymentId, e);
            throw new RuntimeException("Failed to delete payment: " + e.getMessage());
        }
    }
}