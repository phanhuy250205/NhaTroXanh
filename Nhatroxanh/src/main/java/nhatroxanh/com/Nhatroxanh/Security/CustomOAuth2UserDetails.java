package nhatroxanh.com.Nhatroxanh.Security;

import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

// ✅ THAY ĐỔI QUAN TRỌNG: extends CustomUserDetails
public class CustomOAuth2UserDetails extends CustomUserDetails implements OAuth2User, OidcUser {

    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    // Constructor for OAuth2 (e.g., Facebook)
    public CustomOAuth2UserDetails(Users user, Map<String, Object> attributes) {
        // ✅ Gọi constructor của cha, truyền userCccd là null
        super(user, null);
        this.attributes = attributes;
        this.idToken = null;
        this.userInfo = null;
    }

    // Constructor for OIDC (e.g., Google)
    public CustomOAuth2UserDetails(Users user, Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        // ✅ Gọi constructor của cha, truyền userCccd là null
        super(user, null);
        this.attributes = attributes;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    // --- Các phương thức của OAuth2User và OidcUser ---

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    // Ghi đè phương thức getName để đảm bảo tính nhất quán
    @Override
    public String getName() {
        return super.getUser().getEmail();
    }
}