package nhatroxanh.com.Nhatroxanh.Service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import nhatroxanh.com.Nhatroxanh.Model.Dto.VoucherDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.VoucherStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;

public interface VoucherService {
    Page<Vouchers> getActiveVouchers(Pageable pageable);
    Page<Vouchers> getVouchersByStatus(Boolean status, Pageable pageable);
    Page<Vouchers> getVouchersByUserId(Integer userId, Pageable pageable);
    List<Vouchers> getActiveVouchersByHostelId(Integer hostelId);
    Vouchers createVoucher(VoucherDTO voucherDTO, CustomUserDetails userDetails);
    Vouchers updateVoucher(Integer voucherId, VoucherDTO voucherDTO, CustomUserDetails userDetails);
    void deleteVoucher(Integer voucherId, CustomUserDetails userDetails);
    Optional<Vouchers> getVoucherById(Integer voucherId);
    Page<Vouchers> searchVouchersByStatus(Users user, String keyword, Boolean status, Pageable pageable);
    boolean isVoucherCodeExists(String code);
    String generateUniqueVoucherCode();
    void createVoucherHost(Vouchers voucher, Integer ownerId);
    List<Vouchers> getVouchersByOwnerId(Integer ownerId);
    Page<Vouchers> getVouchersByOwnerIdWithFilters(Integer ownerId, String searchQuery, String statusFilter, Pageable pageable);
    void deleteVoucherByIdHost(Integer voucherId, Integer ownerId);
    Vouchers getVoucherByIdAndHost(Integer voucherId, Integer hostId);
    void updateVoucherHost(Integer voucherId, Integer hostId, String title, String code, Integer hostelId, Float discountValue, Integer quantity, Float minAmount, Date startDate, Date endDate, String description, Boolean status);
    boolean existsByCode(String code);
    boolean existsByCodeAndNotId(String code, Integer id);
    void checkAndDeactivateVouchersIfNeeded();
    Page<Vouchers> searchVouchers(Users user, String keyword, Pageable pageable);
    void sendVoucherNotification(Vouchers voucher, CustomUserDetails userDetails);
}