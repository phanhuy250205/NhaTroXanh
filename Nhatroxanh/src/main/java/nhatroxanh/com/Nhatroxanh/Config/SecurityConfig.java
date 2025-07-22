package nhatroxanh.com.Nhatroxanh.Config;

import jakarta.servlet.http.HttpServletResponse;
import nhatroxanh.com.Nhatroxanh.Security.CustomLoginSuccessHandler;
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
    private CustomLoginSuccessHandler customLoginSuccessHandler;

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
                .csrf(csrf -> csrf.disable()) // CSRF disabled for API endpoints
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/**", "/api/**", "/css/**", "/js/**", "/images/**", "/bootstrap/**",
                                "/fonts/**",
                                "/uploads/**")
                        .permitAll()
                        .requestMatchers("/", "/index", "/trang-chu", "/phong-tro/**", "/chi-tiet/**", "/danh-muc/**",
                                "/khach-thue/**", "/infor-chutro", "/khach-thue/thanh-toan", "/voucher", "/momo/**",
                                "/vnpay/**",
                                "/tat-ca-phong-tro")
                        .permitAll()
                        .requestMatchers("/dang-ky-chi-tiet", "/hoan-tat-dang-ky").permitAll()
                        .requestMatchers("/dang-ky-chu-tro", "/dang-nhap-chu-tro", "/infor-chu-tro").permitAll()
                        .requestMatchers("/chu-tro/**").hasRole("OWNER")
                        .requestMatchers("/nhan-vien/**").hasRole("STAFF")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/dang-nhap-chu-tro")
                        .loginProcessingUrl("/login-processing")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(customLoginSuccessHandler) // <-- ĐÃ SỬA: Chỉ giữ lại trình xử lý đúng
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("text/plain; charset=UTF-8");
                            response.getWriter().write("Tên đăng nhập hoặc mật khẩu không chính xác.");
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/perform_logout")
                        .logoutSuccessUrl("/?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll())
                .rememberMe(remember -> remember
                        .tokenRepository(persistentTokenRepository())
                        .key("NhaTroXanhSecretKeyRememberMe")
                        .tokenValiditySeconds(5 * 24 * 60 * 60));

        return http.build();
    }
}