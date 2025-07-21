package nhatroxanh.com.Nhatroxanh.Security;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new UsernameNotFoundException("Tên đăng nhập không được để trống");
        }

        Users user = null;

        // Tìm theo email
        user = userRepository.findByEmail(identifier).orElse(null);

        // Nếu không tìm thấy, tìm theo số điện thoại
        if (user == null) {
            user = userRepository.findByPhone(identifier).orElse(null);
        }

        // Nếu không tìm thấy, tìm theo CCCD từ UserCccd
        if (user == null) {
            Optional<UserCccd> userCccd = userCccdRepository.findByCccdNumber(identifier);
            if (userCccd.isPresent()) {
                user = userCccd.get().getUser();
            }
        }

        // Nếu vẫn không tìm thấy, throw exception
        if (user == null) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng với thông tin: " + identifier);
        }

        // Kiểm tra tài khoản có được kích hoạt không
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Tài khoản chưa được kích hoạt");
        }
        
        // if (user.getStatus() != Users.Status.APPROVED) {
        //     throw new UsernameNotFoundException("Tài khoản đang chờ duyệt");
        // }
        // Tìm UserCccd tương ứng
        UserCccd userCccd = userCccdRepository.findByUser_UserId(user.getUserId()).orElse(null);

        return new CustomUserDetails(user, userCccd);

    }
}