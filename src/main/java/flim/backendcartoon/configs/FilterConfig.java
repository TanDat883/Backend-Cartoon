package flim.backendcartoon.configs;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Filter;

@Configuration
public class FilterConfig {
    // tương tự class security, đăng ký filter cho các route cần bảo vệ
    // dùng nimbus-jose-jwt để xác thực JWT từ Cognito mạnh mẽ và bảo mật cao
    @Bean
    public FilterRegistrationBean<Filter> jwtAuthFilter(CognitoJwtAuthFilter filter) {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns(
                  // Cập nhật phim
                "/movies/delete"       // Xóa phim
        );
        return registrationBean;
    }
}
