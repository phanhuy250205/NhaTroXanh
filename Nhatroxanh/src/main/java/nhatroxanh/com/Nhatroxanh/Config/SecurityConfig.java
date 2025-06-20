package nhatroxanh.com.Nhatroxanh.Config;

import jakarta.servlet.http.HttpServletResponse;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CustomUserDetailsService customUserDetailsService;
        @Autowired
        private DataSource dataSource;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(customUserDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public PersistentTokenRepository persistentTokenRepository() {
                JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
                tokenRepository.setDataSource(dataSource);
                return tokenRepository;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                // API đăng ký/xác thực phải được phép
                                                .requestMatchers("/api/users/**").permitAll()
                                                // ✅ THÊM DÒNG NÀY - Cho phép tất cả API filter
                                                .requestMatchers("/api/**").permitAll()
                                                // Các tài nguyên tĩnh và trang công khai được phép
                                                .requestMatchers("/", "/css/**", "/js/**", "/images/**",
                                                                "/bootstrap/**",
                                                                "/fonts/**", "/uploads/**")
                                                .permitAll()
                                                .requestMatchers("/phong-tro/**", "/chi-tiet/**", "/danh-muc/**",
                                                                "/dang-ky-chu-tro", "/dang-nhap-chu-tro")
                                                .permitAll()
                                                .requestMatchers("/chu-tro/**").hasRole("OWNER") // Chỉ cho phép chủ trọ
                                                                                                 // truy cập
                                                // Mọi request khác cần xác thực
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                // URL xử lý đăng nhập mà JS sẽ gọi
                                                .loginProcessingUrl("/perform_login")
                                                // Xử lý khi đăng nhập AJAX thành công
                                                .successHandler((request, response, authentication) -> {
                                                        response.setStatus(HttpServletResponse.SC_OK);
                                                        response.getWriter().flush();
                                                })
                                                // Xử lý khi đăng nhập AJAX thất bại
                                                .failureHandler((request, response, exception) -> {
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Trả
                                                                                                                 // về
                                                                                                                 // lỗi
                                                                                                                 // 401
                                                        response.setContentType("text/plain; charset=UTF-8"); // Đặt mã
                                                                                                              // hóa
                                                                                                              // UTF-8
                                                        response.getWriter().write(
                                                                        "Vui lòng kiểm tra lại email/mật khẩu, hoặc tài khoản của bạn chưa được kích hoạt.");
                                                        response.getWriter().flush();
                                                })
                                                .permitAll())
                                .logout(logout -> logout
                                                // URL mà form đăng xuất sẽ gửi yêu cầu POST đến
                                                .logoutUrl("/perform_logout")

                                                // URL chuyển hướng đến sau khi đăng xuất thành công
                                                .logoutSuccessUrl("/?logout=true")

                                                // Xóa cookie để kết thúc phiên làm việc hoàn toàn
                                                .deleteCookies("JSESSIONID", "remember-me")

                                                .permitAll())
                                .rememberMe(remember -> remember
                                                .tokenRepository(persistentTokenRepository())
                                                .key("NhaTroXanhSecretKeyRememberMe")
                                                .tokenValiditySeconds(5 * 24 * 60 * 60) // 5 ngày
                                );

                return http.build();
        }
}
