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
    
    // Address fields
    private String province;
    private String district;
    private String ward;
    private String street;
    private String houseNumber;
}
