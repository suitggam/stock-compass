package com.stock.survive.controller;

import com.stock.survive.dto.GameLatestResultDto;
import com.stock.survive.service.GameResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameController {

    private final GameResultService gameResultService;

    // /api/games/latest?user_no="{user_no}"
    @GetMapping("/latest")
    public ResponseEntity<GameLatestResultDto> getLatest(@RequestParam("user_no") Integer userNo) {
        GameLatestResultDto body = gameResultService.getLatestByUserNo(userNo);
        return ResponseEntity.ok(body);
    }
}

