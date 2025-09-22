package com.stock.survive.controller;

import com.stock.survive.dto.MyPageDto;
import com.stock.survive.service.MyPageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

    private final MyPageQueryService myPageQueryService;

    @GetMapping("/me")
    public MyPageDto me(Authentication auth) {
        return myPageQueryService.getMyPage(extractUid(auth));
    }

    // 메인/리스트 별표(★) 표시에 최적 (선택)
    @GetMapping("/me/favorite-ids")
    public Set<Integer> favoriteIds(Authentication auth) {
        return myPageQueryService.getFavoriteIdSet(extractUid(auth));
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