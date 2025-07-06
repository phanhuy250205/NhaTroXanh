package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.sql.Date;
import java.util.Optional;

import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserOwnerRequest;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import nhatroxanh.com.Nhatroxanh.Repository.AddressRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
// import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // @Autowired
    // private OtpService otpService;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Override
    @Transactional
    public Users registerNewUser(UserRequest userRequest) {
        logger.info("Registering new user with email: {}", userRequest.getEmail());
        if (userRepository.findByEmail(userRequest.getEmail()).isPresent()) {
            logger.error("Email already exists: {}", userRequest.getEmail());
            throw new RuntimeException("Email đã được sử dụng!");
        }

        if (userRequest.getCccd() != null && !userRequest.getCccd().trim().isEmpty()) {
            if (userCccdRepository.findByCccdNumber(userRequest.getCccd()).isPresent()) {
                logger.error("CCCD already exists: {}", userRequest.getCccd());
                throw new RuntimeException("Số CCCD đã được sử dụng!");
            }
        }

        Users newUser = new Users();
        newUser.setFullname(userRequest.getFullName());
        newUser.setEmail(userRequest.getEmail());
        newUser.setPhone(userRequest.getPhoneNumber());
        newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        newUser.setEnabled(false);
        newUser.setRole(Users.Role.CUSTOMER);

        Users savedUser = userRepository.save(newUser);
        logger.info("Saved new user with ID: {}", savedUser.getUserId());

        if (userRequest.getCccd() != null && !userRequest.getCccd().trim().isEmpty()) {
            UserCccd userCccd = new UserCccd();
            userCccd.setUser(savedUser);
            userCccd.setCccdNumber(userRequest.getCccd());
            try {
                if (userRequest.getIssueDate() != null && !userRequest.getIssueDate().isEmpty()) {
                    userCccd.setIssueDate(Date.valueOf(userRequest.getIssueDate()));
                }
                userCccd.setIssuePlace(userRequest.getIssuePlace());
                userCccdRepository.save(userCccd);
                logger.info("Saved UserCccd for userId: {}", savedUser.getUserId());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid CCCD issue date format: {}", userRequest.getIssueDate(), e);
                throw new RuntimeException("Định dạng ngày cấp CCCD không hợp lệ: " + userRequest.getIssueDate());
            }
        }

        // otpService.createAndSendOtp(savedUser);

        return savedUser;
    }

    @Override
    @Transactional
    public Users registerOwner(UserOwnerRequest userOwnerRequest) {
        logger.info("Registering new owner with email: {}", userOwnerRequest.getEmail());
        if (userRepository.findByEmail(userOwnerRequest.getEmail()).isPresent()) {
            logger.error("Email already exists: {}", userOwnerRequest.getEmail());
            throw new RuntimeException("Email đã được sử dụng!");
        }

        if (userOwnerRequest.getCccd() != null && !userOwnerRequest.getCccd().trim().isEmpty()) {
            if (userCccdRepository.findByCccdNumber(userOwnerRequest.getCccd()).isPresent()) {
                logger.error("CCCD already exists: {}", userOwnerRequest.getCccd());
                throw new RuntimeException("Số CCCD đã được sử dụng!");
            }
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
                logger.error("Invalid birth date format: {}", userOwnerRequest.getBirthDate(), e);
                throw new RuntimeException("Định dạng ngày sinh không hợp lệ: " + userOwnerRequest.getBirthDate());
            }
        }
        newUser.setRole(Users.Role.OWNER);
        newUser.setEnabled(true);

        Users savedUser = userRepository.save(newUser);
        logger.info("Saved new owner with ID: {}", savedUser.getUserId());

        if (userOwnerRequest.getCccd() != null && !userOwnerRequest.getCccd().trim().isEmpty()) {
            UserCccd userCccd = new UserCccd();
            userCccd.setUser(savedUser);
            userCccd.setCccdNumber(userOwnerRequest.getCccd());
            try {
                if (userOwnerRequest.getIssueDate() != null && !userOwnerRequest.getIssueDate().isEmpty()) {
                    userCccd.setIssueDate(Date.valueOf(userOwnerRequest.getIssueDate()));
                }
                userCccd.setIssuePlace(userOwnerRequest.getIssuePlace());
                userCccdRepository.save(userCccd);
                logger.info("Saved UserCccd for owner with userId: {}", savedUser.getUserId());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid CCCD issue date format: {}", userOwnerRequest.getIssueDate(), e);
                throw new RuntimeException("Định dạng ngày cấp CCCD không hợp lệ: " + userOwnerRequest.getIssueDate());
            }
        }
        return savedUser;
    }

    @Override

    public Users findOwnerByCccdOrPhone(Authentication authentication, String cccd, String phone) {
        logger.info("Finding owner with CCCD: {} or phone: {}", cccd, phone);
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Authentication is null or not authenticated");
            throw new IllegalArgumentException("Không có thông tin xác thực!");
        }
        if ((cccd == null || cccd.trim().isEmpty()) && (phone == null || phone.trim().isEmpty())) {
            logger.error("Both CCCD and phone are empty");
            throw new IllegalArgumentException("Phải cung cấp ít nhất một trong số CCCD hoặc số điện thoại!");
        }

        boolean isOwner = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_OWNER"));
        if (!isOwner) {
            logger.error("User does not have OWNER role");
            throw new IllegalArgumentException("Người dùng không có vai trò OWNER!");
        }

        return userRepository.findByCccdOrPhoneAndRole(cccd, phone, Users.Role.OWNER)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy chủ trọ với CCCD hoặc số điện thoại cung cấp!"));
    }

    @Override
    public UserCccd findUserCccdByUserId(Integer userId) {
        logger.info("Finding UserCccd for userId: {}", userId);
        if (userId == null || userId <= 0) {
            logger.error("Invalid user ID: {}", userId);
            throw new IllegalArgumentException("ID người dùng không hợp lệ!");
        }

        Optional<UserCccd> userCccdOptional = userCccdRepository.findByUser_UserId(userId);
        if (userCccdOptional.isPresent()) {
            logger.info("Found UserCccd for userId: {}", userId);
        } else {
            logger.warn("No UserCccd found for userId: {}", userId);
        }
        return userCccdOptional.orElse(null);
    }

    @Override
    public Optional<Address> findAddressByUserId(Integer userId) {
        logger.info("Finding Address for userId: {}", userId);
        if (userId == null || userId <= 0) {
            logger.error("Invalid user ID: {}", userId);
            throw new IllegalArgumentException("ID người dùng không hợp lệ!");
        }

        Optional<Address> addressOptional = addressRepository.findByUserUserId(userId);
        if (addressOptional.isPresent()) {
            logger.info("Found Address for userId: {}", userId);
        } else {
            logger.warn("No Address found for userId: {}", userId);
        }
        return addressOptional;
    }

    @Override
    @Transactional
    public Users saveUser(Users user) {
        logger.info("Saving user with ID: {}", user.getUserId());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public UserCccd saveUserCccd(UserCccd userCccd) {
        logger.info("Saving UserCccd for userId: {}", userCccd.getUser().getUserId());
        return userCccdRepository.save(userCccd);
    }

    @Override
    @Transactional
    public Address saveAddress(Address address) {
        return addressRepository.save(address);
    }

    public Page<Users> getAllCustomers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Integer> customerIds = userRepository.findCustomerIds(Users.Role.CUSTOMER, pageable);
        List<Users> users = userRepository.findCustomersWithDetails(customerIds.getContent());
        return new PageImpl<>(users, pageable, customerIds.getTotalElements());
    }
}