package nhatroxanh.com.Nhatroxanh.Security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

import java.util.Collection;
import java.util.Collections;
import java.sql.Date;

public class CustomUserDetails implements UserDetails {
    private final Users user;
    private final UserCccd userCccd;

    public CustomUserDetails(Users user, UserCccd userCccd) {
        this.user = user;
        this.userCccd = userCccd;
    }

    public Users getUser() {
        return user;
    }

    public Integer getUserId() {
        return user.getUserId();
    }

    public String getFullName() {
        return user.getFullname();
    }

    public String getCccd() {
        return userCccd != null ? userCccd.getCccdNumber() : null;
    }

    public String getPhone() {
        return user.getPhone();
    }

    public String getAvatar() {
        return user.getAvatar();
    }

    public String getCccdNumber() {
        return userCccd != null ? userCccd.getCccdNumber() : null;
    }

    public Date getIssueDate() {
        return userCccd != null ? userCccd.getIssueDate() : null;
    }

    public String getIssuePlace() {
        return userCccd != null ? userCccd.getIssuePlace() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name().toUpperCase()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}