package flim.backendcartoon.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.POST, "/movies/create").authenticated()
                        .requestMatchers(HttpMethod.POST, "/movies/delete").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/*/update").authenticated()
                        .requestMatchers(HttpMethod.POST, "/movies/*/publish").authenticated()
                        .requestMatchers(HttpMethod.POST,
                                "/episodes/season/*/ep/*/subtitles").authenticated()
                        .requestMatchers(HttpMethod.DELETE,
                                "/episodes/season/*/ep/*/subtitles/*").authenticated()
                        .requestMatchers(HttpMethod.POST,  "/reports/playback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reports/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/reports/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/export/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/data-analyzer/revenue/**").authenticated()

                        .requestMatchers("/payment/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/feedback/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/wishlist/add").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/wishlist/remove").authenticated()
                        .requestMatchers(HttpMethod.GET, "/wishlist/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/promotions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/promotions/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/movies/*/increment-view").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ai/chat").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/ai/welcome").permitAll()
                        .requestMatchers("/data-analyzer/movies/**").permitAll()
                        .requestMatchers("/proxy/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/authors/**").permitAll()
                        .requestMatchers("/users/**").permitAll()
                        .requestMatchers("/episodes/**").permitAll()
                        .requestMatchers("/seasons/**").permitAll()

                        // WebSocket endpoints - QUAN TRỌNG cho Watch Together
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/app/**").permitAll()
                        .requestMatchers("/topic/**").permitAll()
                        .requestMatchers("/queue/**").permitAll()
                        .requestMatchers("/api/watchrooms/**").permitAll()

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/movies/**").permitAll() // Move this after the authenticated matchers

                        .anyRequest().permitAll()
                )
                // Sử dụng xác thực JWT từ Cognito
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000","https://frontend-cartoon-azure.vercel.app"));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // đủ, không cần gọi 2 lần
        configuration.setExposedHeaders(List.of("*")); // nếu thực sự cần
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
