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

}