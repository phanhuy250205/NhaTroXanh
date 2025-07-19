package nhatroxanh.com.Nhatroxanh.Model.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.sql.Date;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

@Data
public class LandlordDetailsRequest {
    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotNull(message = "Giới tính không được để trống")
    private Boolean gender;

        @NotNull(message = "Ngày sinh không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthDate;

    @NotBlank(message = "Số CCCD không được để trống")
    @Pattern(regexp = "^\\d{12}$", message = "Số CCCD phải có 12 chữ số")
    private String cccdNumber;

    @NotNull(message = "Ngày cấp không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date issueDate;

    @NotBlank(message = "Nơi cấp không được để trống")
    private String issuePlace;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @NotNull(message = "Ảnh CCCD mặt trước không được để trống")
    private MultipartFile frontId;

    @NotNull(message = "Ảnh CCCD mặt sau không được để trống")
    private MultipartFile backId;
}