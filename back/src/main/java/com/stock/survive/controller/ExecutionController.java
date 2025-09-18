package com.stock.survive.controller;

import com.stock.survive.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
@Slf4j
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping("/process/{stockNo}")
    public ResponseEntity<Map<String, Object>> processOrdersForStock(
            @PathVariable Integer stockNo,
            @RequestParam Integer currentPrice) {
        log.info("주문 처리 요청: stockNo={}, currentPrice={}", stockNo, currentPrice);
        
        try {
            executionService.processPendingOrdersForStock(stockNo, currentPrice);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "주문 처리가 완료되었습니다",
                    "stockNo", stockNo,
                    "currentPrice", currentPrice
            ));
        } catch (Exception e) {
            log.error("주문 처리 중 오류: stockNo={}, error={}", stockNo, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "주문 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulatePriceChange(
            @RequestParam String ticker,
            @RequestParam Integer newPrice) {
        log.info("주가 변경 시뮬레이션: ticker={}, newPrice={}", ticker, newPrice);
        
        try {
            // 실제로는 StockRealtimeService.updateStockPrice()를 호출
            // 여기서는 시뮬레이션용 엔드포인트
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "주가 변경 시뮬레이션이 완료되었습니다",
                    "ticker", ticker,
                    "newPrice", newPrice
            ));
        } catch (Exception e) {
            log.error("주가 변경 시뮬레이션 중 오류: ticker={}, error={}", ticker, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "주가 변경 시뮬레이션 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}
