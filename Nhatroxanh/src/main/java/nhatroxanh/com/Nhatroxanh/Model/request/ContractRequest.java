package nhatroxanh.com.Nhatroxanh.Model.request;
import java.sql.Date;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractRequest {
    
    @NotNull(message = "Room ID không được để trống")
    private Integer roomId;
    
    @NotNull(message = "User ID không được để trống") 
    private Integer userId;
    
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private Date startDate;
    
    @NotNull(message = "Ngày kết thúc không được để trống")
    private Date endDate;
    
    @NotNull(message = "Giá thuê không được để trống")
    @Positive(message = "Giá thuê phải lớn hơn 0")
    private Float price;
    
    @NotNull(message = "Tiền cọc không được để trống")
    @Positive(message = "Tiền cọc phải lớn hơn 0")
    private Float deposit;
    
    private String terms;
    
    private Boolean status = true;
    
    private List<String> imageUrls;
}