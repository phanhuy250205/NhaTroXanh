package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import nhatroxanh.com.Nhatroxanh.Model.enity.DetailPayments;

import java.util.List;

@Repository
public interface DetailPaymentsRepository extends JpaRepository<DetailPayments, Integer> {
    
    // Lấy chi tiết thanh toán theo payment ID
    List<DetailPayments> findByPaymentId(Integer paymentId);
    
    // Xóa chi tiết thanh toán theo payment ID
    void deleteByPaymentId(Integer paymentId);
    
    // Lấy chi tiết thanh toán theo payment ID và item name
    @Query("SELECT dp FROM DetailPayments dp WHERE dp.payment.id = :paymentId AND dp.itemName = :itemName")
    DetailPayments findByPaymentIdAndItemName(Integer paymentId, String itemName);
}
