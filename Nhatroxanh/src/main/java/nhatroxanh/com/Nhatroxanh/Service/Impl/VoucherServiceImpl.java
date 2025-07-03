package nhatroxanh.com.Nhatroxanh.Service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Repository.VoucherRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.VoucherService;

@Service
public class VoucherServiceImpl implements VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Override
    public Page<Vouchers> getActiveVouchers(Pageable pageable) {
        return voucherRepository.findByStatusTrue(pageable);
    }

    @Override
    public Page<Vouchers> getPendingVouchers(Pageable pageable) {
        return voucherRepository.findByStatusFalse(pageable);
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

}