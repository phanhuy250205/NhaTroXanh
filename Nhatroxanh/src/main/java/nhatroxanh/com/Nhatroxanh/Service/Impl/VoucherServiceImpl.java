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
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
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
    private HostelService hostelService;

    @Autowired
    private EmailService emailService;

    @Override
    public Page<Vouchers> getActiveVouchers(Pageable pageable) {
        return voucherRepository.findByStatusTrue(pageable);
    }

    @Override
    public Page<Vouchers> getVouchersByStatus(Boolean status, Pageable pageable) {
        return voucherRepository.findByStatus(status, pageable);
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
    public Vouchers createVoucher(VoucherDTO dto, CustomUserDetails userDetails) {
        if (voucherRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Mã voucher đã tồn tại!");
        }

        Users user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        Vouchers voucher = Vouchers.builder()
                .code(dto.getCode())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .discountValue(dto.getDiscountValue().floatValue())
                .startDate(Date.valueOf(dto.getStartDate()))
                .endDate(Date.valueOf(dto.getEndDate()))
                .quantity(dto.getQuantity())
                .minAmount(dto.getMinAmount() != null ? dto.getMinAmount().floatValue() : null)
                .user(user)
                .status(true)
                .createdAt(Date.valueOf(LocalDate.now()))
                .build();

        return voucherRepository.save(voucher);
    }

    @Override
    public Vouchers updateVoucher(Integer id, VoucherDTO dto, CustomUserDetails userDetails) {
        Vouchers existing = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        if (!existing.getUser().getUserId().equals(userDetails.getUserId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa!");
        }

        if (!existing.getCode().equals(dto.getCode()) &&
                voucherRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Mã voucher đã tồn tại!");
        }

        existing.setCode(dto.getCode());
        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setDiscountValue(dto.getDiscountValue().floatValue());
        existing.setStartDate(Date.valueOf(dto.getStartDate()));
        existing.setEndDate(Date.valueOf(dto.getEndDate()));
        existing.setQuantity(dto.getQuantity());
        existing.setMinAmount(dto.getMinAmount() != null ? dto.getMinAmount().floatValue() : null);

        Date today = new java.sql.Date(System.currentTimeMillis());

        boolean isExpired = existing.getEndDate().before(today);
        boolean isOut = existing.getQuantity() == 0;
        existing.setStatus(!isExpired && !isOut);

        return voucherRepository.save(existing);
    }

    @Override
    public void deleteVoucher(Integer id, CustomUserDetails userDetails) {
        Vouchers voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        if (!voucher.getUser().getUserId().equals(userDetails.getUserId())) {
            throw new RuntimeException("Bạn không có quyền xóa voucher này!");
        }

        voucherRepository.delete(voucher);
    }

    @Override
    public Optional<Vouchers> getVoucherById(Integer id) {
        return voucherRepository.findById(id);
    }

    @Override
    public Page<Vouchers> searchVouchers(String keyword, Pageable pageable) {
        return voucherRepository.searchVouchers(keyword, pageable);
    }

    @Override
    public void createVoucherHost(Vouchers voucher, Integer ownerId) {
        Hostel hostel = voucher.getHostel();
        if (hostel == null || !hostel.getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("Khu trọ không thuộc quyền quản lý của bạn.");
        }

        if (voucherRepository.existsByCode(voucher.getCode())) {
            throw new IllegalArgumentException("Mã voucher đã tồn tại.");
        }

        if (voucher.getDiscountValue() <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0.");
        }
        if (voucher.getQuantity() <= 0) {
            throw new IllegalArgumentException("Số lượng voucher phải lớn hơn 0.");
        }

        voucherRepository.save(voucher);
    }

    @Override
    public List<Vouchers> getVouchersByOwnerId(Integer ownerId) {
        return voucherRepository.findByUserUserId(ownerId);
    }

    @Override
    public Page<Vouchers> getVouchersByOwnerIdWithFilters(Integer ownerId, String searchQuery, String statusFilter,
            Pageable pageable) {
        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchQuery = "%" + searchQuery.toLowerCase() + "%";
        }

        Boolean status = null;
        if ("active".equalsIgnoreCase(statusFilter)) {
            status = true;
        } else if ("expired".equalsIgnoreCase(statusFilter)) {
            status = false;
        }

        return voucherRepository.findByUserUserIdWithFilters(ownerId, searchQuery, status, pageable);
    }

    @Override
    public void deleteVoucherByIdHost(Integer voucherId, Integer ownerId) {
        Vouchers voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại."));

        if (!voucher.getUser().getUserId().equals(ownerId)) {
            throw new SecurityException("Bạn không có quyền xóa voucher này.");
        }

        voucherRepository.deleteById(voucherId);
    }

    @Override
    public Vouchers getVoucherByIdAndHost(Integer voucherId, Integer hostId) {
        Vouchers voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));
        if (!voucher.getUser().getUserId().equals(hostId)) {
            throw new SecurityException("Không có quyền truy cập voucher này.");
        }
        return voucher;
    }

    @Override
    public void updateVoucherHost(Integer voucherId, Integer hostId, String title, String code,
            Integer hostelId, Float discountValue, Integer quantity, Float minAmount,
            Date startDate, Date endDate, String description, Boolean status) {

        Vouchers voucher = getVoucherByIdAndHost(voucherId, hostId);

        // Normalize code
        String oldCode = voucher.getCode() != null ? voucher.getCode().trim().toLowerCase() : "";
        String newCode = code != null ? code.trim().toLowerCase() : "";

        // Kiểm tra nếu đổi mã mới thì mới kiểm tra trùng
        if (!oldCode.equals(newCode)) {
            if (voucherRepository.existsByCodeAndNotId(newCode, voucherId)) {
                throw new IllegalArgumentException("Mã voucher đã tồn tại. Vui lòng chọn mã khác.");
            }
            voucher.setCode(code.trim()); // Chỉ set lại nếu code khác
        }

        // Cập nhật các trường còn lại
        voucher.setTitle(title != null ? title.trim() : voucher.getTitle());
        voucher.setDiscountValue(discountValue != null ? discountValue : voucher.getDiscountValue());
        voucher.setQuantity(quantity != null ? quantity : voucher.getQuantity());
        voucher.setMinAmount(minAmount != null ? minAmount : voucher.getMinAmount());
        voucher.setStartDate(startDate != null ? startDate : voucher.getStartDate());
        voucher.setEndDate(endDate != null ? endDate : voucher.getEndDate());
        voucher.setDescription(description != null ? description.trim() : voucher.getDescription());
        voucher.setStatus(status != null ? status : voucher.getStatus());

        // Xử lý hostel nếu được gửi lên
        if (hostelId != null) {
            Hostel hostel = hostelService.getHostelById(hostelId)
                    .orElseThrow(() -> new IllegalArgumentException("Khu trọ không tồn tại."));
            if (!hostel.getOwner().getUserId().equals(hostId)) {
                throw new IllegalArgumentException("Khu trọ không thuộc quyền quản lý của bạn.");
            }
            voucher.setHostel(hostel);
        }

        voucherRepository.save(voucher);
    }

    @Override
    public boolean existsByCodeAndNotId(String code, Integer id) {
        return voucherRepository.existsByCodeAndNotId(code, id);
    }

    @Override
    public boolean existsByCode(String code) {
        return voucherRepository.existsByCode(code);
    }

    public void checkAndDeactivateVouchersIfNeeded() {
        List<Vouchers> activeVouchers = voucherRepository.findByStatus(true);
        Date today = new Date(System.currentTimeMillis());

        for (Vouchers voucher : activeVouchers) {
            boolean isExpired = voucher.getEndDate().before(today);
            boolean isDepleted = voucher.getQuantity() != null && voucher.getQuantity() <= 0;

            if (isExpired || isDepleted) {
                voucher.setStatus(false); // Ngừng hoạt động
                voucherRepository.save(voucher); // Cập nhật lại

                // Lý do gửi mail
                String reason;
                if (isExpired && isDepleted) {
                    reason = "Voucher đã hết hạn và số lượng đã về 0.";
                } else if (isExpired) {
                    reason = "Voucher đã hết hạn.";
                } else {
                    reason = "Số lượng voucher đã về 0.";
                }

                // Thông tin gửi mail
                Users owner = voucher.getHostel().getOwner();
                if (owner != null && owner.getEmail() != null) {
                    emailService.sendVoucherDeactivatedEmail(
                            owner.getEmail(),
                            owner.getFullname(),
                            voucher.getTitle(),
                            reason);
                }
            }
        }
    }

}