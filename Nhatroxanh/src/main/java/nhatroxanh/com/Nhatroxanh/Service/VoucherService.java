package nhatroxanh.com.Nhatroxanh.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;

public interface VoucherService {
    Page<Vouchers> getActiveVouchers(Pageable pageable);

    Page<Vouchers> getPendingVouchers(Pageable pageable);

    void deleteVoucher(Integer voucherId, CustomUserDetails userDetails);
}
