package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.Optional;

import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.request.UserOwnerRequest;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import nhatroxanh.com.Nhatroxanh.Repository.AddressRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import nhatroxanh.com.Nhatroxanh.Service.FileUploadService;
import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private UserCccdRepository userCccdReponsitory;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional
    public Users registerNewUser(UserRequest userRequest) {
        logger.info("Registering new user with email: {}", userRequest.getEmail());
        if (userRepository.findByEmail(userRequest.getEmail()).isPresent()) {
            logger.error("Email already exists: {}", userRequest.getEmail());
            throw new RuntimeException("Email đã được sử dụng!");
        }
        if (userRepository.findByPhone(userRequest.getPhoneNumber()).isPresent()) {
            logger.error("Phone number already exists: {}", userRequest.getPhoneNumber());
            throw new RuntimeException("Số điện thoại đã được sử dụng!");
        }
        Users newUser = new Users();
        newUser.setFullname(userRequest.getFullName());
        newUser.setEmail(userRequest.getEmail());
        newUser.setPhone(userRequest.getPhoneNumber());
        newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        newUser.setEnabled(false);
        newUser.setRole(Users.Role.CUSTOMER);
        newUser.setStatus(Users.Status.APPROVED);
        newUser.setCreatedAt(LocalDateTime.now());

        Users savedUser = userRepository.save(newUser);
        logger.info("Saved new user with ID: {}", savedUser.getUserId());
        otpService.createAndSendOtp(savedUser);

        return savedUser;
    }

    @Transactional
    public Users registerOwner(UserOwnerRequest userOwnerRequest, MultipartFile frontImage, MultipartFile backImage)
            throws IOException {
        logger.info("Registering new owner with email: {}", userOwnerRequest.getEmail());

        // Kiểm tra email trùng lặp
        if (userRepository.findByEmail(userOwnerRequest.getEmail()).isPresent()) {
            logger.error("Email already exists: {}", userOwnerRequest.getEmail());
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // Kiểm tra số điện thoại trùng lặp
        if (userRepository.findByPhone(userOwnerRequest.getPhoneNumber()).isPresent()) {
            logger.error("Phone number already exists: {}", userOwnerRequest.getPhoneNumber());
            throw new RuntimeException("Số điện thoại đã được sử dụng!");
        }

        // Kiểm tra số CCCD trùng lặp
        if (userCccdReponsitory.findByCccdNumber(userOwnerRequest.getCccdNumber()).isPresent()) {
            logger.error("CCCD number already exists: {}", userOwnerRequest.getCccdNumber());
            throw new RuntimeException("Số CCCD đã được sử dụng!");
        }

        // Tạo user mới
        Users newUser = new Users();
        newUser.setFullname(userOwnerRequest.getFullName());
        newUser.setEmail(userOwnerRequest.getEmail());
        newUser.setPhone(userOwnerRequest.getPhoneNumber());
        newUser.setPassword(passwordEncoder.encode(userOwnerRequest.getPassword()));
        newUser.setGender(Boolean.parseBoolean(userOwnerRequest.getGender()));
        newUser.setAddress(userOwnerRequest.getAddress());

        // Xử lý ngày sinh
        if (userOwnerRequest.getBirthDate() != null && !userOwnerRequest.getBirthDate().isEmpty()) {
            try {
                newUser.setBirthday(Date.valueOf(userOwnerRequest.getBirthDate()));
            } catch (IllegalArgumentException e) {
                logger.error("Invalid birth date format: {}", userOwnerRequest.getBirthDate(), e);
                throw new RuntimeException("Định dạng ngày sinh không hợp lệ: " + userOwnerRequest.getBirthDate());
            }
        }

        newUser.setRole(Users.Role.OWNER);
        newUser.setEnabled(false);
        newUser.setStatus(Users.Status.PENDING);
        newUser.setCreatedAt(LocalDateTime.now());

        // Xử lý CCCD
        UserCccd userCccd = new UserCccd();
        userCccd.setCccdNumber(userOwnerRequest.getCccdNumber());
        userCccd.setIssuePlace(userOwnerRequest.getIssuePlace());
        try {
            userCccd.setIssueDate(Date.valueOf(userOwnerRequest.getIssueDate()));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid issue date format: {}", userOwnerRequest.getIssueDate(), e);
            throw new RuntimeException("Định dạng ngày cấp CCCD không hợp lệ: " + userOwnerRequest.getIssueDate());
        }

        // Xử lý ảnh CCCD
        if (frontImage != null && !frontImage.isEmpty()) {
            String frontImageUrl = fileUploadService.uploadFile(frontImage, "");
            userCccd.setFrontImageUrl(frontImageUrl);
        }
        if (backImage != null && !backImage.isEmpty()) {
            String backImageUrl = fileUploadService.uploadFile(backImage, "");
            userCccd.setBackImageUrl(backImageUrl);
        }

        userCccd.setUser(newUser);
        newUser.setUserCccd(userCccd);

        // Lưu user vào cơ sở dữ liệu
        Users savedUser = userRepository.save(newUser);
        logger.info("Saved new owner with ID: {}", savedUser.getUserId());

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

    public Page<Users> getAllOwner(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Integer> customerIds = userRepository.findCustomerIds(Users.Role.OWNER, pageable);
        List<Users> users = userRepository.findCustomersWithDetails(customerIds.getContent());
        return new PageImpl<>(users, pageable, customerIds.getTotalElements());
    }

    public Page<Users> getFilteredOwners(String keyword, String statusFilter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullname").ascending());
        Boolean enabled = null;

        if ("active".equalsIgnoreCase(statusFilter)) {
            enabled = true;
        } else if ("inactive".equalsIgnoreCase(statusFilter)) {
            enabled = false;
        }

        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        return userRepository.searchOwners(
                Users.Role.OWNER,
                Users.Status.APPROVED,
                keyword,
                enabled,
                pageable);
    }

    public Page<Users> getStaffUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Integer> customerIds = userRepository.findCustomerIds(Users.Role.STAFF, pageable);
        List<Users> users = userRepository.findCustomersWithDetails(customerIds.getContent());
        return new PageImpl<>(users, pageable, customerIds.getTotalElements());

    }

    @Override
    public Users getById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public Page<Users> searchAndFilterStaffUsers(int page, int size, String keyword, String status) {
        Pageable pageable = PageRequest.of(page, size);
        keyword = keyword == null ? "" : keyword.trim();

        if ("active".equalsIgnoreCase(status)) {
            return userRepository.findByRoleAndEnabledAndKeyword(Users.Role.STAFF, true, keyword, pageable);
        } else if ("inactive".equalsIgnoreCase(status)) {
            return userRepository.findByRoleAndEnabledAndKeyword(Users.Role.STAFF, false, keyword, pageable);
        } else {
            return userRepository.findByRoleAndKeyword(Users.Role.STAFF, keyword, pageable);
        }
    }

    public Optional<Users> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Page<Users> getPendingOwners(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.isEmpty()) {
            return userRepository.findPendingOwnersBySearch(Users.Role.OWNER, Users.Status.PENDING, search, pageable);
        }
        return userRepository.findByRoleAndStatus(Users.Role.OWNER, Users.Status.PENDING, pageable);
    }

    @Transactional
    public void approveOwner(int id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
        if (!Users.Role.OWNER.equals(user.getRole()) || !Users.Status.PENDING.equals(user.getStatus())) {
            throw new RuntimeException("Người dùng không hợp lệ để phê duyệt.");
        }
        user.setStatus(Users.Status.APPROVED);
        user.setEnabled(true);
        userRepository.save(user);
        emailService.sendOwnerApprovalEmail(user.getEmail(), user.getFullname());
        logger.info("Approved owner with ID: {}", id);
    }

    public void rejectOwner(int id) {
        Optional<Users> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Không tìm thấy người dùng để từ chối.");
        }

        Users user = optionalUser.get();
        if (user.getRole() != Users.Role.OWNER || user.getStatus() != Users.Status.PENDING) {
            throw new RuntimeException("Người dùng không hợp lệ để từ chối.");
        }
        emailService.sendOwnerRejectionEmail(user.getEmail(), user.getFullname());
        UserCccd userCccd = userCccdRepository.findByUser(user);
        if (userCccd != null) {
            userCccdRepository.delete(userCccd);
        }
        userRepository.delete(user);
    }

}