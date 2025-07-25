package nhatroxanh.com.Nhatroxanh.Security;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Model.entity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCccdRepository userCccdRepository;

   @Override
   public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    System.out.println("🟡 Đang đăng nhập với email: " + email);
    if (email == null || email.trim().isEmpty()) {
        throw new UsernameNotFoundException("Email không được để trống");
    }
    Users user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

    if (!user.isEnabled()) {
        throw new UsernameNotFoundException("Tài khoản chưa được kích hoạt");
    }

    UserCccd userCccd = userCccdRepository.findByUser_UserId(user.getUserId()).orElse(null);
     System.out.println("✅ Tìm thấy user: " + user.getEmail());
    System.out.println("🔐 Password từ DB (BCrypt): " + user.getPassword());
    System.out.println("🟢 Enabled: " + user.isEnabled());
    return new CustomUserDetails(user, userCccd);
}
}