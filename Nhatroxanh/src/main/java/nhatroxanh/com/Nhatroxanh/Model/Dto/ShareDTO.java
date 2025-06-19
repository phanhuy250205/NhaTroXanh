package nhatroxanh.com.Nhatroxanh.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareDTO {
    private Integer postId;
    private String shareUrl;
    private String title;
    private String description;
    private String imageUrl;
    private String price;
    private boolean success;
    private String message;

    // Constructor for success response
    public ShareDTO(Integer postId, String shareUrl, String title, String description, String imageUrl, String price) {
        this.postId = postId;
        this.shareUrl = shareUrl;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.success = true;
        this.message = "Thành công";
    }

    // Constructor for error response
    public ShareDTO(Integer postId, String message) {
        this.postId = postId;
        this.success = false;
        this.message = message;
    }
}
