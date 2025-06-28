package nhatroxanh.com.Nhatroxanh.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Kiểm tra identifier có null hoặc empty không
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new UsernameNotFoundException("Tên đăng nhập không được để trống");
        }

        Users user = null;
        
        // Tìm theo email
        user = userRepository.findByEmail(identifier).orElse(null);
        
        // Nếu không tìm thấy, tìm theo CCCD
        if (user == null) {
            user = userRepository.findByCccd(identifier).orElse(null);
        }
        
        // Nếu vẫn không tìm thấy, tìm theo số điện thoại
        if (user == null) {
            user = userRepository.findByPhone(identifier).orElse(null);
        }
        
        // Nếu vẫn không tìm thấy, throw exception
        if (user == null) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng với thông tin: " + identifier);
        }
        
        // Kiểm tra tài khoản có được kích hoạt không
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Tài khoản chưa được kích hoạt");
        }
        
        return new CustomUserDetails(user);
    }
}