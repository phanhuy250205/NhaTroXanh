package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class HostInfoDTO {
    private String fullname;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthday;

    private Boolean gender;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    private String phone;


    private String address;

    private String avatar; // đường dẫn ảnh đại diện (nếu có)

    private MultipartFile avatarFile; // file upload ảnh đại diện
    private String cccdNumber;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date issueDate;

    private String issuePlace;

    // (Tuỳ chọn mở rộng) nếu muốn upload ảnh CCCD
    private String frontImageUrl;
    private String backImageUrl;

    private MultipartFile frontCccdFile;
    private MultipartFile backCccdFile;
}
