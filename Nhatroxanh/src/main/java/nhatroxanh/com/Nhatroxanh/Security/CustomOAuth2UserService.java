package nhatroxanh.com.Nhatroxanh.Security;

import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        System.out.println("==========================================================");
        System.out.println("ATTRIBUTES FROM FACEBOOK: " + attributes);
        System.out.println("==========================================================");

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatarUrl = null;
        if (email == null) {
            // Bạn có thể xử lý chuyên nghiệp hơn ở đây, ví dụ chuyển hướng về trang
            // yêu cầu nhập email thủ công
            throw new OAuth2AuthenticationException(
                    "Facebook không cung cấp địa chỉ email. Vui lòng kiểm tra lại tài khoản Facebook của bạn.");
        }
        if ("google".equalsIgnoreCase(provider)) {
            avatarUrl = (String) attributes.get("picture");
        } else if ("facebook".equalsIgnoreCase(provider)) {
            if (attributes.containsKey("picture")) {
                Map<String, Object> pictureObj = (Map<String, Object>) attributes.get("picture");
                if (pictureObj.containsKey("data")) {
                    Map<String, Object> dataObj = (Map<String, Object>) pictureObj.get("data");
                    avatarUrl = (String) dataObj.get("url");
                }
            }
        }

        // Logic xử lý user vẫn giữ nguyên
        Optional<Users> userOptional = userRepository.findByEmail(email);
        Users user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setFullname(name);
            user.setAvatar(avatarUrl);
            // Quan trọng: Cập nhật provider nếu người dùng đã tồn tại nhưng đăng nhập bằng
            // cách khác
            user.setAuthProvider(Users.AuthProvider.valueOf(provider.toUpperCase()));
        } else {
            user = Users.builder()
                    .email(email)
                    .fullname(name)
                    .avatar(avatarUrl)
                    .role(Users.Role.CUSTOMER)
                    .authProvider(Users.AuthProvider.valueOf(provider.toUpperCase()))
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .balance(0.0)
                    .build();
        }
        userRepository.save(user);

        // ✅ THAY ĐỔI QUAN TRỌNG: Trả về đối tượng CustomOAuth2UserDetails mới
        return new CustomOAuth2UserDetails(user, attributes);
    }
}