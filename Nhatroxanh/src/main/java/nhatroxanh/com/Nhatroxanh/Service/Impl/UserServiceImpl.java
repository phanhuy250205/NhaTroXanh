package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.sql.Date;

import java.util.Optional;

import java.util.List;

import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Repository.AddressRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserOwnerRequest;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
// import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

import static com.mysql.cj.conf.PropertyKey.logger;

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

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Transactional
    public Users registerNewUser(UserRequest userRequest) {
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(userRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        otpService.createAndSendOtp(savedUser);

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

    @Override
    public UserCccd findUserCccdByUserId(Integer userId) {
        System.out.println("=== START: Finding UserCccd for userId: " + userId + " ===");
        if (userId == null || userId <= 0) {
            System.out.println("❌ Invalid user ID: " + userId);
            throw new IllegalArgumentException("ID người dùng không hợp lệ!");
        }

        Optional<UserCccd> userCccdOptional = userRepository.findUserCccdByUserId(userId);
        if (userCccdOptional.isPresent()) {
            System.out.println("✅ Found UserCccd: " + userCccdOptional.get());
        } else {
            System.out.println("❌ No UserCccd found for userId: " + userId);
        }
        System.out.println("=== END: Finding UserCccd for userId: " + userId + " ===");
        return userCccdOptional.orElse(null);
    }

    @Override
    public Optional<Address> findAddressByUserId(Integer userId) {
        System.out.println("=== START: Finding Address for userId: " + userId + " ===");
        if (userId == null || userId <= 0) {
            System.out.println("❌ Invalid user ID: " + userId);
            throw new IllegalArgumentException("ID người dùng không hợp lệ!");
        }

        Optional<Address> addressOptional = userRepository.findAddressByUserId(userId);
        if (addressOptional.isPresent()) {
            System.out.println("✅ Found Address: " + addressOptional.get());
        } else {
            System.out.println("❌ No Address found for userId: " + userId);
        }
        System.out.println("=== END: Finding Address for userId: " + userId + " ===");
        return addressOptional;
    }

    @Override
    @Transactional
    public Users saveUser(Users user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public UserCccd saveUserCccd(UserCccd userCccd) {
        return userCccdRepository.save(userCccd); // Sử dụng UserCccdRepository
    }

    @Override
    @Transactional
    public Address saveAddress(Address address) {
        return addressRepository.save(address); // Sử dụng AddressRepository
    }

}