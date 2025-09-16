package com.stock.survive.security.filter;

import com.stock.survive.util.JWTUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JWTCheckFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/users/auth/",    // /users/auth/kakao, /users/auth/kakao/callback, /users/auth/refresh ...
            "/error",
            "/actuator/health",
            "/favicon.ico"
    );

    private boolean isWhitelisted(HttpServletRequest req) {
        String path = req.getRequestURI();
        for (String p : WHITELIST_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return isWhitelisted(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);

            // uid는 Number로 올 수 있음 → int로 안전 변환
            Integer uid = (claims.get("uid") instanceof Number)
                    ? ((Number) claims.get("uid")).intValue()
                    : Integer.valueOf(String.valueOf(claims.get("uid")));

            String role = String.valueOf(claims.get("role"));
            var auth = new UsernamePasswordAuthenticationToken(
                    uid, null, List.of(new SimpleGrantedAuthority(role)));

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            write401(response, "ERROR_ACCESS_TOKEN_EXPIRED", e.getMessage());
        } catch (JwtException e) { // Malformed/Unsupported/Signature 등
            write401(response, "ERROR_ACCESS_TOKEN_INVALID", e.getMessage());
        }
    }

    private void write401(HttpServletResponse res, String code, String msg) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = """
                {"error":"%s","message":"%s"}
                """.formatted(code, msg == null ? "" : msg.replace("\"","'"));
        res.getWriter().write(json);
    }
}
