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
    Page<Vouchers> searchVouchers(String keyword, Pageable pageable);

    // Kiểm tra mã voucher đã tồn tại chưa
    boolean isVoucherCodeExists(String code);

    // Sinh mã code tự động
    String generateUniqueVoucherCode();

}
