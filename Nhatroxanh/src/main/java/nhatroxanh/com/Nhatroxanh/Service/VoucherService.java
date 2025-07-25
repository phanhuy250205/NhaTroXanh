package nhatroxanh.com.Nhatroxanh.Service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import nhatroxanh.com.Nhatroxanh.Model.Dto.VoucherDTO;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Model.entity.VoucherStatus;
import nhatroxanh.com.Nhatroxanh.Model.entity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;

public interface VoucherService {
        // Lấy danh sách voucher đang hoạt động
        Page<Vouchers> getActiveVouchers(Pageable pageable);

        // Lấy danh sách voucher theo trạng thái true/false
        Page<Vouchers> getVouchersByStatus(Boolean status, Pageable pageable);

        // Lấy voucher theo người dùng
        Page<Vouchers> getVouchersByUserId(Integer userId, Pageable pageable);

        // Lấy danh sách voucher đang hoạt động theo hostel
        List<Vouchers> getActiveVouchersByHostelId(Integer hostelId);

        // Tạo mới voucher từ DTO
        Vouchers createVoucher(VoucherDTO voucherDTO, CustomUserDetails userDetails);

        // Cập nhật voucher
        Vouchers updateVoucher(Integer voucherId, VoucherDTO voucherDTO, CustomUserDetails userDetails);

        // Xóa voucher
        void deleteVoucher(Integer voucherId, CustomUserDetails userDetails);

        // Tìm voucher theo ID
        Optional<Vouchers> getVoucherById(Integer voucherId);

        // Tìm kiếm voucher theo từ khóa
        Page<Vouchers> searchVouchersByStatus(Users user, String keyword, Boolean status, Pageable pageable);

        // Kiểm tra mã voucher đã tồn tại chưa
        boolean isVoucherCodeExists(String code);

        // Sinh mã code tự động
        String generateUniqueVoucherCode();

        void createVoucherHost(Vouchers voucher, Integer ownerId);

        List<Vouchers> getVouchersByOwnerId(Integer ownerId);

        Page<Vouchers> getVouchersByOwnerIdWithFilters(Integer ownerId, String searchQuery, String statusFilter,
                        Pageable pageable);

        void deleteVoucherByIdHost(Integer voucherId, Integer ownerId);

        Vouchers getVoucherByIdAndHost(Integer voucherId, Integer hostId);

        void updateVoucherHost(Integer voucherId, Integer hostId, String title, String code,
                        Integer hostelId, Float discountValue, Integer quantity, Float minAmount,
                        Date startDate, Date endDate, String description, Boolean status);

        boolean existsByCode(String code);

        boolean existsByCodeAndNotId(String code, Integer id);

        void checkAndDeactivateVouchersIfNeeded();

        Page<Vouchers> searchVouchers(Users user, String keyword, Pageable pageable);
}
