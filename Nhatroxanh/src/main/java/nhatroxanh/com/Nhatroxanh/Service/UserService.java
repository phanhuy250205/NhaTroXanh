package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserOwnerRequest;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import org.springframework.security.core.Authentication;

public interface UserService {

    /**
     * Dùng để đăng ký một người dùng mới.
     * Phương thức này sẽ xử lý việc kiểm tra email, mã hóa mật khẩu,
     * lưu người dùng vào cơ sở dữ liệu với trạng thái chưa kích hoạt,
     * và gọi dịch vụ để gửi mã OTP.
     *
     * @param userRequest Đối tượng chứa thông tin đăng ký từ người dùng.
     * @return Đối tượng Users sau khi đã được lưu.
     */
    Users registerNewUser(UserRequest userRequest);

    /**
     * Dùng để đăng ký một chủ trọ mới.
     * Phương thức này sẽ xử lý việc kiểm tra email, mã hóa mật khẩu,
     * lưu người dùng vào cơ sở dữ liệu với vai trò ROLE_OWNER,
     * và gọi dịch vụ để gửi mã OTP.
     *
     * @param userOwnerRequest Đối tượng chứa thông tin đăng ký của chủ trọ.
     * @return Đối tượng Users sau khi đã được lưu.
     */
    Users registerOwner(UserOwnerRequest userOwnerRequest);

    /**
     * Tìm người dùng dựa trên username.
     *
     * @param username Tên đăng nhập của người dùng.
     * @return Đối tượng Users nếu tìm thấy, null nếu không tìm thấy.
     */
     Users findOwnerByCccdOrPhone(Authentication authentication, String cccd, String phone);
}