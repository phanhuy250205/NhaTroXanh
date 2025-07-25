package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.entity.DetailPayments;

import java.util.List;

@Repository
public interface DetailPaymentsRepository extends JpaRepository<DetailPayments, Integer> {

    // Lấy chi tiết thanh toán theo payment ID
    List<DetailPayments> findByPaymentId(Integer paymentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM DetailPayments dp WHERE dp.payment.id = :paymentId")
    void deleteByPaymentId(@Param("paymentId") Integer paymentId);

    // Lấy chi tiết thanh toán theo payment ID và item name
    @Query("SELECT dp FROM DetailPayments dp WHERE dp.payment.id = :paymentId AND dp.itemName = :itemName")
    DetailPayments findByPaymentIdAndItemName(Integer paymentId, String itemName);
}
