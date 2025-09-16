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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/users")
//public class AuthController {
//
//    private final KakaoOAuthService kakao;
//    private final UserLinkService linker;
//    private final TokenService tokenService;
//
//    /** 카카오 OAuth 시작 */
//    @GetMapping("/auth/kakao")
//    public void kakaoLogin(HttpServletResponse res) throws Exception {
//        res.sendRedirect(kakao.buildAuthorizeUrl());
//    }
//
//    /** 카카오 콜백: 유저 연결/생성 + 토큰 발급(Access 바디, Refresh 쿠키) */
//    @GetMapping("/auth/kakao/callback")
//    public ResponseEntity<AuthResponse> kakaoCallback(
//            @RequestParam String code,
//            @RequestParam String state,
//            HttpServletResponse res
//    ) {
//        kakao.verifyState(state);
//        var info = kakao.exchangeAndFetchUser(code);
//        User user = linker.linkOrCreateByProvider(info);
//
//        var pair = tokenService.issue(user);           // Access + Refresh 생성
//        tokenService.setRefreshCookie(res, pair.refresh()); // Refresh를 httponly 쿠키로 내려줌
//        return ResponseEntity.ok(new AuthResponse(pair.access(), UserSummaryDto.of(user)));
//    }
//
//    /** Access 재발급: Refresh 쿠키 검증/로테이션 후 새 Access 반환 */
//    @PostMapping("/auth/refresh")
//    public Map<String, String> refresh(HttpServletRequest req, HttpServletResponse res) {
//        String newAccess = tokenService.refreshFromCookie(req, res);
//        return Map.of("accessToken", newAccess);
//    }
//
//    /** 로그아웃: Refresh 폐기(서버/쿠키) */
//    @PostMapping("/logout")
//    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
//        tokenService.revokeFromCookie(req, res);
//        return ResponseEntity.noContent().build();
//    }
//}

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class AuthController {

    private final KakaoOAuthService kakao;
    private final UserLinkService linker;
    private final TokenService tokenService;

    @Value("${app.front-redirect:http://localhost:5173}")
    private String frontBase;

    //일단 테스트용으로 로그인 페이지로 넘어가게 하려고 테스트용
    @Value("${app.front-redirect-after:/login}")
    private String frontAfter;

    @GetMapping("/auth/kakao")
    public void kakaoLogin(HttpServletResponse res) throws Exception {
        res.sendRedirect(kakao.buildAuthorizeUrl());
    }

    @GetMapping("/auth/kakao/callback")
    public void kakaoCallback(@RequestParam String code,
                              @RequestParam String state,
                              HttpServletResponse res) throws Exception {
        // 1) state 검증 (얜 잘 모르겠다)
        kakao.verifyState(state);

        // 2) 토큰 교환,사용자 정보 조회
        var info = kakao.exchangeAndFetchUser(code);

        User user = linker.linkOrCreateByProvider(info);

        // 4) Access / Refresh 발급하구 Refresh를 HttpOnly 쿠키로 세팅
        var pair = tokenService.issue(user);
        tokenService.setRefreshCookie(res, pair.refresh());

        // 5) 리다이렉트하는데 테스트용으로 /login 으로 넘어가게 설정 나중에 어떻게 할지 생각
        String base = frontBase.replaceAll("/+$", "");
        String after = frontAfter.startsWith("/") ? frontAfter : ("/" + frontAfter);
        res.sendRedirect(base + after);
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

