package com.stock.survive.controller;

import com.stock.survive.service.GoogleOAuthService;
import com.stock.survive.service.KakaoOAuthService;
import com.stock.survive.service.TokenService;
import com.stock.survive.service.UserLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class AuthController {

    private final KakaoOAuthService kakao;
    private final GoogleOAuthService google;
    private final UserLinkService linker;
    private final TokenService tokenService;

    // ★ 이제 이것만 사용
    @Value("${app.front-origin:http://localhost:5173}")
    private String frontOrigin;

    /** 항상 홈(/)로 리다이렉트하는 URL 생성 */
    private String frontHome() {
        // origin 기반으로 path=/ 만 보장
        return UriComponentsBuilder.fromUriString(frontOrigin)
                .replacePath("/")     // 무조건 홈
                .replaceQuery(null)
                .fragment(null)
                .build()
                .toUriString();
    }

    // ===== 구글 =====
    @GetMapping("/auth/google")
    public void googleLogin(HttpServletResponse res) throws Exception {
        res.sendRedirect(google.buildAuthorizeUrl());
    }

    @GetMapping("/auth/google/callback")
    public void googleCallback(@RequestParam String code,
                               @RequestParam String state,
                               HttpServletResponse res) throws Exception {
        google.verifyState(state);
        var info = google.exchangeAndFetchUser(code);
        var user = linker.linkOrCreateByProvider(info);

        var pair = tokenService.issue(user);
        tokenService.setRefreshCookie(res, pair.refresh());

        res.sendRedirect(frontHome());
    }

    // ===== 카카오 =====
    @GetMapping("/auth/kakao")
    public void kakaoLogin(HttpServletResponse res) throws Exception {
        res.sendRedirect(kakao.buildAuthorizeUrl());
    }

    @GetMapping("/auth/kakao/callback")
    public void kakaoCallback(@RequestParam String code,
                              @RequestParam String state,
                              HttpServletResponse res) throws Exception {
        kakao.verifyState(state);
        var info = kakao.exchangeAndFetchUser(code);
        var user = linker.linkOrCreateByProvider(info);

        var pair = tokenService.issue(user);
        tokenService.setRefreshCookie(res, pair.refresh());

        res.sendRedirect(frontHome());
    }

    /** Access 재발급: Refresh 쿠키 검증/로테이션 후 새 Access 반환 */
    @PostMapping("/auth/refresh")
    public Map<String, String> refresh(HttpServletRequest req, HttpServletResponse res) {
        String newAccess = tokenService.refreshFromCookie(req, res);
        return Map.of("accessToken", newAccess);
    }

    /** 로그아웃: Refresh 폐기(서버/쿠키) */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        tokenService.revokeFromCookie(req, res);
        return ResponseEntity.noContent().build();
    }
}
