package nhatroxanh.com.Nhatroxanh.Security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {
    private final Users user;

    public CustomUserDetails(Users user) {
        this.user = user;
    }   

    public Users getUser() {
        return user;
    }

    public Integer getUserId() {
        return user.getUserId(); // Hoặc user.getId() nếu cột là id
    }
    // ✅ THÊM MỚI


    public String getFullName() {
        return user.getFullname();
    }
public String getCccd() {
        return user.getCccd();
    }

    public String getPhone() {
        return user.getPhone();
    }
    public String getAvatar() {
        return user.getAvatar();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Lấy vai trò của người dùng và chuyển nó thành một đối tượng GrantedAuthority
        // Thêm tiền tố "ROLE_" theo quy ước của Spring Security
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name().toUpperCase()));
    }

    @Override
    public String getPassword() {
        // Trả về mật khẩu đã được mã hóa của người dùng
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // ### BỔ SUNG QUAN TRỌNG ###
        // Trả về email của người dùng, vì chúng ta đã dùng email làm định danh chính
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        // Tài khoản không bao giờ hết hạn
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Tài khoản không bị khóa
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Thông tin xác thực (mật khẩu) không bao giờ hết hạn
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Trả về trạng thái kích hoạt của tài khoản
        return user.isEnabled();
    }
}
