package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostelDTO {
    private Integer hostelId;
    private String name;
    private String description;
    private Boolean status;
    private Integer roomNumber;
    private Integer ownerId;

    private String province;     // Mã tỉnh (code)
    private String provinceName; // Tên tỉnh
    private String district;
    private String districtName;
    private String ward;
    private String wardName;
    private String street;
    private String houseNumber;
}
