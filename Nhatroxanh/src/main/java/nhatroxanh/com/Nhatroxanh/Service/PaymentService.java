package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentRequestDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.PaymentResponseDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.Payments.PaymentStatus;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    
    /**
     * Lấy tất cả payments của owner (không phân trang - để tương thích ngược)
     */
    List<PaymentResponseDto> getPaymentsByOwnerId(Integer ownerId);
    
    /**
     * Lấy payments của owner với phân trang
     */
    Page<PaymentResponseDto> getPaymentsByOwnerIdWithPagination(Integer ownerId, Pageable pageable);
    
    /**
     * Lấy 8 payments mới nhất của owner
     */
    List<PaymentResponseDto> getRecentPaymentsByOwnerId(Integer ownerId);
    
    /**
     * Lấy payments theo owner và status (không phân trang - để tương thích ngược)
     */
    List<PaymentResponseDto> getPaymentsByOwnerIdAndStatus(Integer ownerId, PaymentStatus status);
    
    /**
     * Lấy payments theo owner và status với phân trang
     */
    Page<PaymentResponseDto> getPaymentsByOwnerIdAndStatusWithPagination(Integer ownerId, PaymentStatus status, Pageable pageable);
    
    /**
     * Tìm kiếm payments theo từ khóa (không phân trang - để tương thích ngược)
     */
    List<PaymentResponseDto> searchPayments(Integer ownerId, String keyword);
    
    /**
     * Tìm kiếm payments theo từ khóa với phân trang
     */
    Page<PaymentResponseDto> searchPaymentsWithPagination(Integer ownerId, String keyword, Pageable pageable);
    
    /**
     * Lấy chi tiết payment theo ID
     */
    PaymentResponseDto getPaymentById(Integer paymentId);
    
    /**
     * Tạo payment/invoice mới
     */
    PaymentResponseDto createPayment(PaymentRequestDto request);
    
    /**
     * Cập nhật trạng thái payment
     */
    PaymentResponseDto updatePaymentStatus(Integer paymentId, PaymentStatus status);
    
    /**
     * Lấy thống kê payments của owner
     */
    Map<String, Object> getPaymentStatistics(Integer ownerId);
    
    /**
     * Lấy danh sách contracts có thể tạo payment
     */
    List<Map<String, Object>> getAvailableContractsForPayment(Integer ownerId);
    
    /**
     * Tính toán chi phí utilities (điện, nước) với đơn giá từ frontend
     */
    Map<String, Object> calculateUtilityCosts(Integer previousReading, Integer currentReading, String utilityType, Float unitPrice);
    void deletePayment(Integer paymentId);
}
