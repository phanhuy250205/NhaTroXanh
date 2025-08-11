package nhatroxanh.com.Nhatroxanh.Model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserOwnerRequest {
    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(min = 2, message = "Họ tên phải có ít nhất 2 ký tự.")
    private String fullName;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không hợp lệ.")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự.")
    private String password;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số.")
    private String phoneNumber;

    @NotBlank(message = "Ngày sinh không được để trống.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Ngày sinh phải đúng định dạng yyyy-MM-dd.")
    private String birthDate;

    @NotBlank(message = "Giới tính không được để trống.")
    private String gender;

    @NotBlank(message = "Số CCCD không được để trống.")
    @Pattern(regexp = "^[0-9]{12}$", message = "Số CCCD phải có 12 chữ số.")
    private String cccdNumber;

    @NotBlank(message = "Nơi cấp CCCD không được để trống.")
    private String issuePlace;

    @NotBlank(message = "Ngày cấp CCCD không được để trống.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Ngày cấp phải đúng định dạng yyyy-MM-dd.")
    private String issueDate;

    private String frontImageUrl;

    private String backImageUrl;

    @NotBlank(message = "Địa chỉ không được để trống.")
    private String address;
}