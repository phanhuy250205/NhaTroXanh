package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.VoucherDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.VoucherStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.VoucherRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.VoucherService;
import nhatroxanh.com.Nhatroxanh.Util.VoucherCodeGenerator;

@Service
@Transactional
public class VoucherServiceImpl implements VoucherService {
    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private RoomsRepository roomRepository;

    @Override
    public Page<Vouchers> getActiveVouchers(Pageable pageable) {
        return voucherRepository.findByVoucherStatus(VoucherStatus.APPROVED, pageable);
    }

    // Cập nhật method getPendingVouchers để lấy voucher PENDING
    @Override
    public Page<Vouchers> getPendingVouchers(Pageable pageable) {
        return voucherRepository.findByVoucherStatus(VoucherStatus.PENDING, pageable);
    }

    @Override
    public void deleteVoucher(Integer voucherId, CustomUserDetails userDetails) {
        Vouchers voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        if (voucher.getUser() == null) {
            throw new RuntimeException("Voucher không có thông tin người tạo!");
        }

        if (!voucher.getUser().getUserId().equals(userDetails.getUserId())) {
            throw new RuntimeException("Bạn không có quyền xóa voucher này!");
        }

        voucherRepository.delete(voucher);
    }

    @Override
    public Vouchers createVoucher(VoucherDTO voucherDTO, CustomUserDetails userDetails) {
        // Validate voucher code uniqueness
        if (voucherRepository.existsByCode(voucherDTO.getCode())) {
            throw new RuntimeException("Mã voucher đã tồn tại!");
        }

        // Get user
        Users user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        // Get hostel if specified
        Hostel hostel = null;
        if (voucherDTO.getHostelId() != null) {
            hostel = hostelRepository.findById(voucherDTO.getHostelId())
                    .orElseThrow(() -> new RuntimeException("Khu trọ không tồn tại!"));
        }

        // Get room if specified
        Rooms room = null;
        if (voucherDTO.getRoomId() != null) {
            room = roomRepository.findById(voucherDTO.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Phòng không tồn tại!"));
        }

        // Create voucher
        Vouchers voucher = Vouchers.builder()
                .code(voucherDTO.getCode())
                .title(voucherDTO.getTitle())
                .description(voucherDTO.getDescription())
                .discountType(voucherDTO.getDiscountType())
                .discountValue(voucherDTO.getDiscountValue().floatValue())
                .startDate(Date.valueOf(voucherDTO.getStartDate()))
                .endDate(Date.valueOf(voucherDTO.getEndDate()))
                .quantity(voucherDTO.getQuantity())
                .minAmount(voucherDTO.getMinAmount() != null ? voucherDTO.getMinAmount().floatValue() : null)
                .user(user)
                .hostel(hostel) // Can be null
                .room(room) // Can be null
                .voucherStatus(VoucherStatus.PENDING)
                .status(true)
                .createdAt(Date.valueOf(LocalDate.now()))
                .build();

        return voucherRepository.save(voucher);
    }

    @Override
    public Vouchers updateVoucher(Integer voucherId, VoucherDTO voucherDTO, CustomUserDetails userDetails) {
        Vouchers existingVoucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        if (!existingVoucher.getUser().getUserId().equals(userDetails.getUserId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa voucher này!");
        }

        if (!existingVoucher.getCode().equals(voucherDTO.getCode()) &&
                voucherRepository.existsByCode(voucherDTO.getCode())) {
            throw new RuntimeException("Mã voucher đã tồn tại!");
        }

        existingVoucher.setCode(voucherDTO.getCode());
        existingVoucher.setTitle(voucherDTO.getTitle());
        existingVoucher.setDescription(voucherDTO.getDescription());
        existingVoucher.setDiscountType(voucherDTO.getDiscountType());
        existingVoucher.setDiscountValue(voucherDTO.getDiscountValue().floatValue());
        existingVoucher.setStartDate(Date.valueOf(voucherDTO.getStartDate()));
        existingVoucher.setEndDate(Date.valueOf(voucherDTO.getEndDate()));
        existingVoucher.setQuantity(voucherDTO.getQuantity());
        existingVoucher.setMinAmount(voucherDTO.getMinAmount() != null ? voucherDTO.getMinAmount().floatValue() : null);

        if (voucherDTO.getStatus() != null) {
            existingVoucher.setStatus(voucherDTO.getStatus());
        }

        return voucherRepository.save(existingVoucher);
    }

    @Override
    public Optional<Vouchers> getVoucherById(Integer voucherId) {
        return voucherRepository.findById(voucherId);
    }

    @Override
    public Page<Vouchers> searchVouchers(String keyword, Pageable pageable) {
        return voucherRepository.searchVouchers(keyword, pageable);
    }

    @Override
    public Page<Vouchers> getVouchersByStatus(VoucherStatus status, Pageable pageable) {
        return voucherRepository.findByVoucherStatus(status, pageable);
    }

    @Override
    public Page<Vouchers> getVouchersByUserId(Integer userId, Pageable pageable) {
        return voucherRepository.findByUserId(userId, pageable);
    }

    @Override
    public List<Vouchers> getActiveVouchersByHostelId(Integer hostelId) {
        return voucherRepository.findActiveVouchersByHostelId(hostelId);
    }

    @Override
    public void approveVoucher(Integer voucherId, CustomUserDetails userDetails) {
        Vouchers voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        voucher.setVoucherStatus(VoucherStatus.APPROVED);
        voucher.setStatus(true);

        Users approver = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("Người duyệt không tồn tại!"));

        voucher.setApprovedBy(approver);

        voucherRepository.save(voucher);
    }

    @Override
    public void rejectVoucher(Integer voucherId, CustomUserDetails userDetails) {
        Vouchers voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        voucher.setVoucherStatus(VoucherStatus.REJECTED);
        voucher.setStatus(false);
        voucherRepository.save(voucher);
    }

    @Override
    public boolean isVoucherCodeExists(String code) {
        return voucherRepository.existsByCode(code);
    }

    @Override
    public String generateUniqueVoucherCode() {
        String code;
        do {
            code = VoucherCodeGenerator.generateVoucherCode();
        } while (voucherRepository.existsByCode(code));
        return code;
    }

    @Override
    public Page<Vouchers> searchAndFilterVouchers(String keyword, String statusFilter,
            String discountType, VoucherStatus voucherStatus, Pageable pageable) {

        Boolean status = null;
        Boolean discountTypeValue = null;

        if ("Hoạt động".equals(statusFilter))
            status = true;
        else if ("Ngừng hoạt động".equals(statusFilter))
            status = false;

        if ("Phần trăm".equals(discountType))
            discountTypeValue = true;
        else if ("Số tiền cố định".equals(discountType))
            discountTypeValue = false;

        return voucherRepository.findBySearchAndFiltersWithStatus(
                keyword, status, discountTypeValue, voucherStatus, pageable);
    }

}