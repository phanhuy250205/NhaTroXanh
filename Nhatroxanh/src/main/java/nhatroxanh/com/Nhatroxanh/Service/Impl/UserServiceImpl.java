package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.sql.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserOwnerRequest;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private OtpService otpService;

    @Transactional
    public Users registerNewUser(UserRequest userRequest) {
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(userRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        Users newUser = new Users();

        newUser.setFullname(userRequest.getFullName());
        newUser.setEmail(userRequest.getEmail());
        newUser.setPhone(userRequest.getPhoneNumber());
        newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        newUser.setEnabled(false);
        newUser.setRole(Users.Role.CUSTOMER); // Updated to uppercase CUSTOMER

        Users savedUser = userRepository.save(newUser);

        otpService.createAndSendOtp(savedUser);

        return savedUser;
    }

    @Override
    @Transactional
    public Users registerOwner(UserOwnerRequest userOwnerRequest) {
        if (userRepository.findByEmail(userOwnerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng!");
        }
        Users newUser = new Users();
        newUser.setFullname(userOwnerRequest.getFullName());
        newUser.setEmail(userOwnerRequest.getEmail());
        newUser.setPhone(userOwnerRequest.getPhoneNumber());
        newUser.setPassword(passwordEncoder.encode(userOwnerRequest.getPassword()));
        if (userOwnerRequest.getBirthDate() != null && !userOwnerRequest.getBirthDate().isEmpty()) {
            try {
                newUser.setBirthday(Date.valueOf(userOwnerRequest.getBirthDate()));
            } catch (IllegalArgumentException e) {
                System.err.println("Định dạng ngày sinh không hợp lệ: " + userOwnerRequest.getBirthDate());
            }
        }
        newUser.setRole(Users.Role.OWNER); // Updated to uppercase OWNER
        newUser.setEnabled(true);
        Users savedUser = userRepository.save(newUser);
        return savedUser;
    }

    @Override
    public Users findOwnerByCccdOrPhone(Authentication authentication, String cccd, String phone) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Không có thông tin xác thực!");
        }
        if ((cccd == null || cccd.trim().isEmpty()) && (phone == null || phone.trim().isEmpty())) {
            throw new IllegalArgumentException("Phải cung cấp ít nhất một trong số CCCD hoặc số điện thoại!");
        }

        // Kiểm tra vai trò ROLE_OWNER
        boolean isOwner = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_OWNER"));
        if (!isOwner) {
            throw new IllegalArgumentException("Người dùng không có vai trò OWNER!");
        }

        Optional<Users> user = Optional.empty();
        if (cccd != null && !cccd.trim().isEmpty()) {
            user = userRepository.findByCccd(cccd);
        }
        if (user.isEmpty() && phone != null && !phone.trim().isEmpty()) {
            user = userRepository.findByPhone(phone);
        }

        if (!user.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy chủ trọ với CCCD hoặc số điện thoại cung cấp!");
        }

        return user.get();
    }
}