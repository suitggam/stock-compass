package com.stock.survive.controller;

import com.stock.survive.dto.ExtractKeywordsDto;
import com.stock.survive.dto.StockInfosDto;
import com.stock.survive.service.StockInfosService;
import com.stock.survive.serviceImpl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Log4j2
public class StockInfosController {

    private final StockInfosService stockInfosService;
    private final UserServiceImpl userService;

    // 주식 정보 조회
    @GetMapping("/info/{ticker}")
    public ResponseEntity<List<StockInfosDto>> getStockInfo(
            @PathVariable String ticker
    ) {
        List<StockInfosDto> dto = stockInfosService.getStock(ticker);
        return ResponseEntity.ok(dto);
    }


    // 키워드 추출 (외부 API 연동 포함)
    @PostMapping("/extract-keywords/{ticker}")
    public ResponseEntity<ExtractKeywordsDto> extractKeywords(
            @PathVariable("ticker") String ticker,
            @RequestBody ExtractKeywordsDto requestDto
    ) {
        log.info("Extract keywords request: ticker={}, requestDto={}", ticker, requestDto);

        // 서비스에서 외부 API 호출 및 DTO 생성
        ExtractKeywordsDto result = stockInfosService.getKeywords(ticker, requestDto);

        log.info("Extract keywords response: {}", result);
        return ResponseEntity.ok(result);
    }

    // 최신 종가 조회
    @GetMapping("/latest-price/{itemNo}")
    public ResponseEntity<Integer> getLatestEndPrice(@PathVariable("itemNo") Integer itemNo) {
        Integer latestPrice = stockInfosService.getLatestEndPrice(itemNo);
        log.info(latestPrice);
        if (latestPrice == null) {
            return ResponseEntity.notFound().build(); // 데이터 없으면 404
        }
        return ResponseEntity.ok(latestPrice);
    }

    // 즐겨찾기 토글
    @PostMapping("/favorites/toggle")
    public ResponseEntity<Boolean> toggleFavorite(@RequestParam String ticker, Principal principal) {
        if (principal == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        Long userId = Long.parseLong(principal.getName());

        boolean newState = userService.toggleFavorite(userId, ticker);
        return ResponseEntity.ok(newState);
    }


}
