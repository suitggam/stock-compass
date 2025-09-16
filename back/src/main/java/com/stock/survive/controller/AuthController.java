package com.stock.survive.controller;

import com.stock.survive.dto.AuthResponse;
import com.stock.survive.dto.UserSummaryDto;
import com.stock.survive.entity.User;
import com.stock.survive.service.KakaoOAuthService;
import com.stock.survive.service.TokenService;
import com.stock.survive.service.UserLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class AuthController {

    private final KakaoOAuthService kakao;
    private final UserLinkService linker;
    private final TokenService tokenService;

    /** 카카오 OAuth 시작 */
    @GetMapping("/auth/kakao")
    public void kakaoLogin(HttpServletResponse res) throws Exception {
        res.sendRedirect(kakao.buildAuthorizeUrl());
    }

    /** 카카오 콜백: 유저 연결/생성 + 토큰 발급(Access 바디, Refresh 쿠키) */
    @GetMapping("/auth/kakao/callback")
    public ResponseEntity<AuthResponse> kakaoCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse res
    ) {
        kakao.verifyState(state);
        var info = kakao.exchangeAndFetchUser(code);
        User user = linker.linkOrCreateByProvider(info);

        var pair = tokenService.issue(user);           // Access + Refresh 생성
        tokenService.setRefreshCookie(res, pair.refresh()); // Refresh를 httponly 쿠키로 내려줌
        return ResponseEntity.ok(new AuthResponse(pair.access(), UserSummaryDto.of(user)));
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
