package nhatroxanh.com.Nhatroxanh.Model.Dto;


import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
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
    private String address;
    private Date createdAt;

    public String getCombinedAddress() {
        return (houseNumber != null ? houseNumber + " " : "") +
               (street != null ? street + ", " : "") +
               (wardName != null ? wardName + ", " : "") +
               (districtName != null ? districtName + ", " : "") +
               (provinceName != null ? provinceName : "");
    }

    public void parseAddress(String address) {
        if (address != null && !address.isEmpty()) {
            String[] parts = address.split(", ");
            if (parts.length >= 4) {
                this.houseNumber = parts[0].contains(" ") ? parts[0].substring(0, parts[0].indexOf(" ")) : parts[0];
                this.street = parts[0].contains(" ") ? parts[0].substring(parts[0].indexOf(" ") + 1) : "";
                this.wardName = parts[1];
                this.districtName = parts[2];
                this.provinceName = parts[3];
                this.address = address;
            }
        }
    }
}