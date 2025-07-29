package nhatroxanh.com.Nhatroxanh.Security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        // ✅ Đơn giản hóa logic:
        // Bây giờ principal luôn là CustomOAuth2UserDetails
        CustomOAuth2UserDetails oAuth2UserDetails = (CustomOAuth2UserDetails) authentication.getPrincipal();

        // Lấy thông tin Users trực tiếp mà không cần tìm lại trong DB
        Users user = oAuth2UserDetails.getUser();

        // Logic điều hướng giữ nguyên
        String targetUrl = determineTargetUrl(user);

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(Users user) {
        // Lấy vai trò của người dùng
        if (user.getRole() == null) {
            // Xử lý trường hợp role là null cho người dùng mới
            return "/trang-chu"; 
        }
        String role = user.getRole().name();

        // Điều hướng dựa trên vai trò
        if (role.contains("OWNER")) {
            return "/chu-tro/tong-quan";
        } else if (role.contains("STAFF")) {
            return "/nhan-vien/bai-dang";
        } else if (role.contains("ADMIN")) {
            return "/admin/thong-ke";
        }
        // Trang mặc định cho CUSTOMER
        return "/trang-chu";
    }
}