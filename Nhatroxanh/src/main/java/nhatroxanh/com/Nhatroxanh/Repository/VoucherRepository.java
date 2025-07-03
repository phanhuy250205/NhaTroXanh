package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Vouchers;

public interface VoucherRepository extends JpaRepository<Vouchers, Integer> {
    Page<Vouchers> findByStatusTrue(Pageable pageable);

    Page<Vouchers> findByStatusFalse(Pageable pageable);
}
