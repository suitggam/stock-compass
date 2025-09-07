package dev.a301.stock.config;

import dev.a301.stock.jwt.JwtAuthFilter;
import dev.a301.stock.jwt.JwtUtil;
import dev.a301.stock.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler successHandler;
    private final JwtUtil jwt;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 기본 보안 옵션
            .csrf(csrf -> csrf.disable())
            .cors(c -> c.configurationSource(corsSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())

            // 401 처리 (토큰 없거나 잘못된 경우)
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            ))

            // 접근 허용 경로
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/error", "/health", "/public/**",
                    "/oauth2/**",
                    "/users/auth/**",        // 시작 URL
                    "/users/*/callback"      // 콜백 URL
                ).permitAll()
                .anyRequest().authenticated()
            )

            // OAuth2 시작/콜백 경로와 성공/실패 핸들러
            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(a -> a.baseUri("/users/auth"))        // ★ 시작 URL
                .redirectionEndpoint(r -> r.baseUri("/users/*/callback"))     // ★ 콜백 URL
                .successHandler(successHandler)
                .failureHandler((req, res, ex) -> {
                    // 필요시 로깅
                    // System.out.println("OAuth2 failure: " + ex);
                    res.sendRedirect("http://localhost:5173/oauth/fail");
                })
            );

        // JWT 필터 등록 (UsernamePasswordAuthenticationFilter 앞)
        http.addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        for (String o : allowedOrigins.split(",")) {
            String v = o.trim();
            if (!v.isEmpty()) cfg.addAllowedOrigin(v);
        }
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS"));
        cfg.addAllowedHeader("*");
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
