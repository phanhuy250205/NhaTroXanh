package nhatroxanh.com.Nhatroxanh.Model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(min = 2, message = "Họ tên phải có ít nhất 2 ký tự.")
    private String fullName;

     @NotBlank(message = "Email không được để trống.")
    @Pattern(
        regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
        message = "Email không đúng định dạng."
    )
    private String email;

    // <<< THAY ĐỔI QUAN TRỌNG Ở ĐÂY >>>
    @NotBlank(message = "Mật khẩu không được để trống.")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,}$",
        message = "Mật khẩu phải từ 6 ký tự, có ít nhất 1 chữ viết hoa và 1 ký tự đặc biệt."
    )
    private String password;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không đúng định dạng Việt Nam.")
    private String phoneNumber;
}