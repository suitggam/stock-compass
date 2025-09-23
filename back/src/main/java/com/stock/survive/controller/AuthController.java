package com.stock.survive.controller;

import com.stock.survive.serviceImpl.GoogleOAuthServiceImpl;
import com.stock.survive.serviceImpl.KakaoOAuthServiceImpl;
import com.stock.survive.serviceImpl.TokenServiceImpl;
import com.stock.survive.serviceImpl.UserLinkServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class AuthController {

    private final KakaoOAuthServiceImpl kakao;
    private final GoogleOAuthServiceImpl google;
    private final UserLinkServiceImpl linker;
    private final TokenServiceImpl tokenServiceImpl;

    //그냥 로그인한 유저 관리하는것도
    private final com.stock.survive.service.AuthQueryService authQueryService;

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

    @GetMapping("/login-user")
    public com.stock.survive.dto.UserSummaryDto loginUser(Authentication auth) {
        return authQueryService.me(extractUid(auth));
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

        var pair = tokenServiceImpl.issue(user);
        tokenServiceImpl.setRefreshCookie(res, pair.refresh());

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

        var pair = tokenServiceImpl.issue(user);
        tokenServiceImpl.setRefreshCookie(res, pair.refresh());

        res.sendRedirect(frontHome());
    }

    /** Access 재발급: Refresh 쿠키 검증/로테이션 후 새 Access 반환 */
    @PostMapping("/auth/refresh")
    public Map<String, String> refresh(HttpServletRequest req, HttpServletResponse res) {
        String newAccess = tokenServiceImpl.refreshFromCookie(req, res);
        return Map.of("accessToken", newAccess);
    }

    /** 로그아웃: Refresh 폐기(서버/쿠키) */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        tokenServiceImpl.revokeFromCookie(req, res);
        return ResponseEntity.noContent().build();
    }

    private Integer extractUid(Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        Object p = auth.getPrincipal();
        if (p instanceof Integer i) return i;
        if (p instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(p)); } catch (Exception ignored) {}
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_PRINCIPAL");
    }
}
