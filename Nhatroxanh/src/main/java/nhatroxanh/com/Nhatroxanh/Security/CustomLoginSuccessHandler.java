package nhatroxanh.com.Nhatroxanh.Security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String redirectURL = null;

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();

            switch (role) {
                case "ROLE_OWNER":
                    redirectURL = "/chu-tro/tong-quan";
                    break;
                case "ROLE_STAFF":
                    redirectURL = "/nhan-vien/bai-dang";
                    break;
                case "ROLE_ADMIN":
                    redirectURL = "/admin/dashboard";
                    break;
                case "ROLE_CUSTOMER":
                    redirectURL = "/trang-chu"; 
                    break;
            }

            if (redirectURL != null) break;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (redirectURL != null) {
            response.getWriter().write("{\"redirectUrl\": \"" + redirectURL + "\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Bạn không có quyền truy cập vào hệ thống này.\"}");
        }
    }
}




