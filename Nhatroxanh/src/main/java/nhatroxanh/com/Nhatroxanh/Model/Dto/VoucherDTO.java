package nhatroxanh.com.Nhatroxanh.Model.Dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class VoucherDTO {
    @NotBlank(message = "Tên chương trình không được để trống")
    private String title;

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    private String description;

    @NotNull(message = "Loại giảm giá không được để trống")
    private Boolean discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    private Double discountValue;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    @NotNull(message = "Số lượng không được để trống")
    private Integer quantity;

    private Double minAmount;
    
    private List<Integer> hostelIds;

    private Boolean status;
}