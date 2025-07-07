package nhatroxanh.com.Nhatroxanh.Model.Dto;


import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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
    List<String> parts = new ArrayList<>();
    
    // Định nghĩa hàm làm sạch với kiểu Function
    Function<String, String> clean = (text) -> {
        if (text == null) return "";
        return text.replaceAll(",+", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    };

    String cleanHouseNumber = clean.apply(houseNumber);
    String cleanStreet = clean.apply(street);
    String cleanWardName = clean.apply(wardName);
    String cleanDistrictName = clean.apply(districtName);
    String cleanProvinceName = clean.apply(provinceName);

    // Gộp houseNumber và street thành một phần duy nhất
    String addressPart = cleanHouseNumber;
    if (!cleanStreet.isEmpty()) {
        addressPart += (cleanHouseNumber.isEmpty() ? "" : " ") + cleanStreet;
    }

    if (!addressPart.isEmpty()) parts.add(addressPart);
    if (!cleanWardName.isEmpty()) parts.add(cleanWardName);
    if (!cleanDistrictName.isEmpty()) parts.add(cleanDistrictName);
    if (!cleanProvinceName.isEmpty()) parts.add(cleanProvinceName);

    String address = String.join(", ", parts);
    return address.replaceAll(",+", ",").replaceAll("(^,)|(,$)", "").trim();
}

    public void parseAddress(String address) {
        if (address != null && !address.isEmpty()) {
            address = address.replaceAll(",+", ",").replaceAll("(^,)|(,$)", "").trim();
            String[] parts = address.split(",\\s*");
            if (parts.length >= 1) {
                String addressPart = parts[0].trim();
                int firstSpaceIndex = addressPart.indexOf(" ");
                this.houseNumber = firstSpaceIndex > 0 ? addressPart.substring(0, firstSpaceIndex) : "";
                this.street = firstSpaceIndex > 0 ? addressPart.substring(firstSpaceIndex + 1) : addressPart;
                if (parts.length >= 2) this.wardName = parts[1].trim();
                if (parts.length >= 3) this.districtName = parts[2].trim();
                if (parts.length >= 4) this.provinceName = parts[3].trim();
                this.address = address;
            }
        }
    }
}
