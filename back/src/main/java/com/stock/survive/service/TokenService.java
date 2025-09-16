package com.stock.survive.service;

import org.springframework.stereotype.Service;
import com.stock.survive.entity.User;
import com.stock.survive.security.refresh.RefreshTokenStore;
import com.stock.survive.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JWTUtil jwtUtil;
    private final RefreshTokenStore refreshStore;

    @Value("${jwt.access-exp-minutes:60}") private long accessExpMin;
    @Value("${jwt.refresh-exp-days:14}")  private long refreshExpDays;

    public record Pair(String access, String refresh) {}

    public Pair issue(User u) {
        String access = jwtUtil.generateToken(
                Map.of("uid", u.getId(), "email", u.getSocialEmail(),
                        "nickname", u.getNickname(), "role", "ROLE_USER"),
                Duration.ofMinutes(accessExpMin));
        String refresh = refreshStore.issue(u.getId(), Duration.ofDays(refreshExpDays));
        return new Pair(access, refresh);
    }

    public void setRefreshCookie(jakarta.servlet.http.HttpServletResponse res, String refresh) {
        var c = ResponseCookie.from("refresh_token", refresh)
                .httpOnly(true).secure(true)     // 로컬 http면 임시 false
                .sameSite("Lax")                 // 크로스도메인이면 None + Secure
                .path("/")
                .maxAge(Duration.ofDays(refreshExpDays))
                .build();
        res.addHeader("Set-Cookie", c.toString());
    }

    public void clearRefreshCookie(jakarta.servlet.http.HttpServletResponse res) {
        var c = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(0).build();
        res.addHeader("Set-Cookie", c.toString());
    }
}
