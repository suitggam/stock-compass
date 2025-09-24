package com.stock.survive.controller;

import com.stock.survive.dto.ExtractKeywordsDto;
import com.stock.survive.dto.StockInfosDto;
import com.stock.survive.service.StockInfosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Log4j2
public class StockInfosController {

    private final StockInfosService stockInfosService;

    // 주식 정보 조회
    @GetMapping("/info/{ticker}")
    public ResponseEntity<List<StockInfosDto>> getStockInfo(@PathVariable("ticker") String ticker) {
        List<StockInfosDto> response = stockInfosService.getStock(ticker);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<Integer> getLatestEndPrice(@PathVariable("itemNo") Long itemNo) {
        Optional<Integer> latestPriceOptional = stockInfosService.getLatestEndPrice(itemNo);
        
        // Optional 객체에서 값을 가져오고, 값이 없으면 404를 반환하도록 변경
        if (latestPriceOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Integer latestPrice = latestPriceOptional.get();
        log.info(latestPrice);
        
        return ResponseEntity.ok(latestPrice);
    }

}
