package nhatroxanh.com.Nhatroxanh.Security;

import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // QUAN TRỌNG: Phải có dòng import này

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOidcUserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional // QUAN TRỌNG: Annotation này sẽ giải quyết vấn đề
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("picture");

        System.out.println("--- GOOGLE AVATAR URL NHẬN ĐƯỢC: " + avatarUrl + " ---");
        
        Optional<Users> userOptional = userRepository.findByEmail(email);
        Users user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setFullname(name);
            user.setAvatar(avatarUrl); // Cập nhật avatar
            user.setAuthProvider(Users.AuthProvider.GOOGLE);
        } else {
            user = Users.builder()
                    .email(email)
                    .fullname(name)
                    .avatar(avatarUrl)
                    .role(Users.Role.CUSTOMER)
                    .authProvider(Users.AuthProvider.GOOGLE)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .balance(0.0)
                    .build();
        }
        
        userRepository.save(user); // Lệnh save này bây giờ sẽ được commit xuống CSDL

        return new CustomOAuth2UserDetails(user, oidcUser.getAttributes(), oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}