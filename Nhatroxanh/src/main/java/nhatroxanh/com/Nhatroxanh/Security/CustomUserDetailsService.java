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
        // Giữ nguyên logic tìm theo email
        Users user = userRepository.findByEmail(identifier)
                .orElse(null);

        // Thêm logic tìm theo CCCD
        if (user == null) {
            user = userRepository.findByCccd(identifier)
                    .orElse(null);
        }

        // Thêm logic tìm theo số điện thoại
        if (user == null) {
            user = userRepository.findByPhone(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với CCCD, số điện thoại hoặc email: " + identifier));
        }

        return new CustomUserDetails(user);
    }
}