package com.stock.survive.controller.tendency;

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
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(tendencyGameService.finish(userId, request));
    }
    
}
