package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactDTO {
    private Integer postId;
    private String phoneNumber;
    private String ownerName;
    private boolean success;
    private String message;

    // Constructor for success response
    public ContactDTO(Integer postId, String phoneNumber, String ownerName) {
        this.postId = postId;
        this.phoneNumber = phoneNumber;
        this.ownerName = ownerName;
        this.success = true;
        this.message = "Thành công";
    }

    // Constructor for error response
    public ContactDTO(Integer postId, String message) {
        this.postId = postId;
        this.success = false;
        this.message = message;
    }
}
