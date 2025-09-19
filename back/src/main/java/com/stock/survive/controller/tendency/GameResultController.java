package com.stock.survive.controller;

import com.stock.survive.dto.tendency.TendencyGameFinishRequest;
import com.stock.survive.dto.tendency.TendencyGameResultResponse;
import com.stock.survive.service.TendencyGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GameResultController {

    private final TendencyGameService tendencyGameService;

    @PostMapping("/api/games")
    public ResponseEntity<TendencyGameResultResponse> save(Authentication authentication,
                                                           @Valid @RequestBody TendencyGameFinishRequest request) {
        Integer userId = resolveUserId(authentication);
        return ResponseEntity.ok(tendencyGameService.finish(userId, request));
    }

    private Integer resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Integer integer) {
            return integer;
        }
        if (principal instanceof String str) {
            return Integer.parseInt(str);
        }
        throw new IllegalStateException("지원하지 않는 인증 주체입니다.");
    }
}
