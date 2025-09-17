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

    @Value("${app.front-redirect:http://localhost:5173}")
    private String frontBase;

    //일단 테스트용으로 로그인 페이지로 넘어가게 하려고 테스트용
    @Value("${app.front-redirect-after:/login}")
    private String frontAfter;

    // ===== 공통: 프론트 리다이렉트 URL 조립 =====
    private String buildFrontRedirectUrl() {
        String after = frontAfter.startsWith("/") ? frontAfter : ("/" + frontAfter);
        return UriComponentsBuilder.fromUriString(frontBase)           
                .replacePath(null)            
                .path(after)                    
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

        res.sendRedirect(buildFrontRedirectUrl());
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

        res.sendRedirect(buildFrontRedirectUrl());
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

