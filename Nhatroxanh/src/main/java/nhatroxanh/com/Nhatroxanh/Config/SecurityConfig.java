package nhatroxanh.com.Nhatroxanh.Config;

import jakarta.servlet.http.HttpServletResponse;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
            // Bật CSRF cho các form, chỉ tắt cho API nếu cần
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**") // Tắt CSRF cho API
            )
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Cho phép truy cập tài nguyên tĩnh và các trang công khai
                .requestMatchers("/css/**", "/js/**", "/images/**", "/bootstrap/**", "/fonts/**", "/uploads/**").permitAll()
                .requestMatchers("/", "/index", "/trang-chu", "/phong-tro/**", "/chi-tiet/**", "/danh-muc/**").permitAll()
                .requestMatchers("/dang-ky-chu-tro", "/dang-nhap-chu-tro", "/login-processing").permitAll()
                // Chỉ cho phép OWNER truy cập các endpoint hợp đồng
                .requestMatchers("/api/contracts/**").hasAuthority("owner")
                // Chỉ cho phép OWNER truy cập các trang host
                .requestMatchers("/host/**").hasAuthority("owner")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/dang-nhap-chu-tro")
                .loginProcessingUrl("/login-processing")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/api/contracts/form", true) // Redirect về form sau login
                .failureHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("text/plain; charset=UTF-8");
                    response.getWriter().write("Tên đăng nhập hoặc mật khẩu không chính xác.");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/perform_logout")
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .tokenRepository(persistentTokenRepository())
                .key("NhaTroXanhSecretKeyRememberMe")
                .tokenValiditySeconds(5 * 24 * 60 * 60)
            )
            .exceptionHandling(exception -> exception
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendRedirect("/dang-nhap-chu-tro?error=access-denied");
                })
            );

        return http.build();
    }
}