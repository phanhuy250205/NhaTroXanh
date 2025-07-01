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

    private String provinceCode;
    private String provinceName;
    private String districtCode;
    private String districtName;
    private String wardCode;
    private String wardName;
    private String street;
    private String houseNumber; 
}
