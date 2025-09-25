package com.stock.survive.controller;

import com.stock.survive.dto.TradeCardDto;
import com.stock.survive.dto.TradeHistoryDto;
import com.stock.survive.dto.UserAssetDto;
import com.stock.survive.service.TradeHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
@Log4j2
public class TradeHistoryController {

    private final TradeHistoryService tradeHistoryService;

    @GetMapping("/userAsset")
    public ResponseEntity<UserAssetDto> userAsset( Principal principal   ){
        Long userNo = Long.parseLong(principal.getName());
        UserAssetDto userAssetDto =tradeHistoryService.getUserAssets(userNo);
        return ResponseEntity.ok(userAssetDto);
    }

    // 티커, 가격, 수량을 URL 파라미터로 받는 방식
    @PostMapping("/buy/{ticker}")
    public ResponseEntity<TradeHistoryDto> tradeBuy(
            @PathVariable String ticker,           // 티커
            @RequestBody TradeCardDto request,  // 가격과 수량을 포함하는 DTO를 받음
            Principal principal                    // 로그인된 사용자 정보
    ) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            Long userNo = Long.parseLong(principal.getName());  // Principal에서 사용자 ID 가져오기

            // buy 거래를 처리하고 결과 DTO를 반환
            TradeHistoryDto tradeHistoryDto = tradeHistoryService.processBuy(userNo, ticker, request.getPrice(), request.getVolume());

            return ResponseEntity.ok(tradeHistoryDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/sell/{ticker}")
    public ResponseEntity<TradeHistoryDto> tradeSell(
            @PathVariable String ticker,           // 티커
            @RequestBody TradeCardDto request,  // 가격과 수량을 포함하는 DTO를 받음
            Principal principal                    // 로그인된 사용자 정보
    ) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            Long userNo = Long.parseLong(principal.getName());  // Principal에서 사용자 ID 가져오기

            // buy 거래를 처리하고 결과 DTO를 반환
            TradeHistoryDto tradeHistoryDto = tradeHistoryService.processSell(userNo, ticker, request.getPrice(), request.getVolume());

            return ResponseEntity.ok(tradeHistoryDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}

