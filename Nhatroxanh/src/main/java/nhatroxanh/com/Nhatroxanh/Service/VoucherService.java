package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import nhatroxanh.com.Nhatroxanh.Model.Dto.VoucherDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.VoucherStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;

public interface VoucherService {
    Page<Vouchers> getActiveVouchers(Pageable pageable);

    Page<Vouchers> getPendingVouchers(Pageable pageable);

    void deleteVoucher(Integer voucherId, CustomUserDetails userDetails);

    // New methods
    Vouchers createVoucher(VoucherDTO voucherDTO, CustomUserDetails userDetails);

    Vouchers updateVoucher(Integer voucherId, VoucherDTO voucherDTO, CustomUserDetails userDetails);

    Optional<Vouchers> getVoucherById(Integer voucherId);

    Page<Vouchers> searchVouchers(String keyword, Pageable pageable);

    Page<Vouchers> getVouchersByStatus(VoucherStatus status, Pageable pageable);

    Page<Vouchers> getVouchersByUserId(Integer userId, Pageable pageable);

    List<Vouchers> getActiveVouchersByHostelId(Integer hostelId);

    void approveVoucher(Integer voucherId, CustomUserDetails userDetails);

    void rejectVoucher(Integer voucherId, CustomUserDetails userDetails);

    boolean isVoucherCodeExists(String code);

    String generateUniqueVoucherCode();

    Page<Vouchers> searchAndFilterVouchers(String keyword, String statusFilter,
            String discountType, VoucherStatus voucherStatus, Pageable pageable);
}
