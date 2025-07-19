package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;

import java.util.List;

import org.springframework.data.domain.Page;

import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserOwnerRequest;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

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

    /**
     * Tìm thông tin CCCD của người dùng dựa trên user_id.
     *
     * @param userId ID của người dùng trong bảng Users.
     * @return Đối tượng UserCccd nếu tìm thấy, null nếu không tìm thấy.
     */
    UserCccd findUserCccdByUserId(Integer userId);

    Optional<Address> findAddressByUserId(Integer userId);

    Users saveUser(Users user);

    UserCccd saveUserCccd(UserCccd userCccd);

    Address saveAddress(Address address);
    // List<Users> getAllCustomers();

    Page<Users> getAllCustomers(int page, int size);
    Optional<Users> findByEmail(String email);

    Page<Users> getAllOwner(int page, int size);

    Page<Users> getFilteredOwners(String keyword, String statusFilter, int page, int size);

    Page<Users> getStaffUsers(int page, int size);

    Users getById(Integer id);

    Page<Users> searchAndFilterStaffUsers(int page, int size, String keyword, String status);
}