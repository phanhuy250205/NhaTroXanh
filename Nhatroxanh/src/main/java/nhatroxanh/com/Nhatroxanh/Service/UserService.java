package nhatroxanh.com.Nhatroxanh.Service;

import java.util.List;

import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserOwnerRequest;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;

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
    Users registerOwner(UserOwnerRequest userOwnerRequest);
    /**
     * Cung cấp danh sách thông tin khách thuê cho trang quản lý của Chủ trọ.
     * @return một danh sách các đối tượng DTO.
     */
   
}