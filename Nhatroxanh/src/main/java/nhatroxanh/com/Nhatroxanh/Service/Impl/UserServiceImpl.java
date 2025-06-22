package nhatroxanh.com.Nhatroxanh.Service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users.Role;
import nhatroxanh.com.Nhatroxanh.Model.request.UserRequest;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
// import nhatroxanh.com.Nhatroxanh.Service.OtpService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;

@Service
public class UserServiceImpl implements UserService {
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;
  // @Autowired
  // private OtpService otpService;

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
    newUser.setRole(Role.customer); // Mặc định là customer

    Users savedUser = userRepository.save(newUser);

    // otpService.createAndSendOtp(savedUser);

    return savedUser;
  }
}
