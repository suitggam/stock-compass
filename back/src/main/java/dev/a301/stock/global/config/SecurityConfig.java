package dev.a301.stock.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import dev.a301.stock.global.security.jwt.JwtAuthFilter;
import dev.a301.stock.global.security.jwt.JwtUtil;
import dev.a301.stock.global.security.oauth.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler successHandler;
    private final JwtUtil jwtUtil;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.oauth2.redirect-fail:http://localhost:5173/oauth/fail}")
    private String oauthFailRedirect;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 세션/폼로그인 비활성화 + CORS
            .csrf(csrf -> csrf.disable())
            .cors(c -> c.configurationSource(corsSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())

            // 인증 실패(401) 처리
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            ))

            // 경로별 권한
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/error", "/health", "/public/**",
                    "/oauth2/**",                 // 스프링 디폴트 OAuth2 내부 경로
                    "/users/auth/**",             // OAuth2 시작 URL
                    "/users/*/callback",          // OAuth2 콜백 URL
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/logout"
                ).permitAll()
                .anyRequest().authenticated()
            )

            // OAuth2 성공/실패 핸들러
            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(a -> a.baseUri("/users/auth"))         // 시작 URL
                .redirectionEndpoint(r -> r.baseUri("/users/*/callback"))     // 콜백 URL
                .successHandler(successHandler)
                .failureHandler((req, res, ex) -> res.sendRedirect(oauthFailRedirect))
            );

        // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
        http.addFilterBefore(new JwtAuthFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // 허용 Origin (콤마로 여러 개 지정 가능)
        for (String o : allowedOrigins.split(",")) {
            String v = o.trim();
            if (!v.isEmpty()) cfg.addAllowedOrigin(v);
        }

        // 허용 메서드/헤더
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS"));
        cfg.addAllowedHeader("*");

        // 쿠키/인증정보 포함
        cfg.setAllowCredentials(true);

        // (선택) 프론트에서 읽어야 하는 헤더가 있으면 노출 지정
        // cfg.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
