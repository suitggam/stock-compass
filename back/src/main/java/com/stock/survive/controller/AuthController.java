package com.stock.survive.controller;

import com.stock.survive.dto.AuthResponse;
import com.stock.survive.dto.UserSummaryDto;
import com.stock.survive.entity.User;
import com.stock.survive.service.KakaoOAuthService;
import com.stock.survive.service.UserLinkService;
import com.stock.survive.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/users/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoOAuthService kakao;
    private final UserLinkService linker;
    private final TokenService tokenService;

    @GetMapping("/kakao")
    public void kakaoLogin(HttpServletResponse res) throws Exception {
        res.sendRedirect(kakao.buildAuthorizeUrl());
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<AuthResponse> kakaoCallback(
            @RequestParam String code, @RequestParam String state, HttpServletResponse res) {

        kakao.verifyState(state);
        var info = kakao.exchangeAndFetchUser(code);
        User user = linker.linkOrCreateByProvider(info);

        var pair = tokenService.issue(user);
        tokenService.setRefreshCookie(res, pair.refresh());
        return ResponseEntity.ok(new AuthResponse(pair.access(), UserSummaryDto.of(user)));
    }

    // (선택) 구글도 같은 방식으로 /google, /google/callback 추가
}
