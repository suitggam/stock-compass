package com.stock.survive.controller;

import com.stock.survive.dto.StockRealtimeDto;
import com.stock.survive.service.StockRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Log4j2
public class StockRealtimeController {

    private final StockRealtimeService stockRealtimeService;

    // 전체 주식 정보 + 최신 거래 정보 조회
    @GetMapping("/realtime")
    public List<StockRealtimeDto> getStockItems() {
        List<StockRealtimeDto> list = stockRealtimeService.getAllStockRealtimeWithLatestInfo();

        // 로그로 확인
        for (StockRealtimeDto dto : list) {
            log.info("티커: " + dto.getTicker()
                    + " | 거래량: " + dto.getVolume()
                    + " | 시가총액: " + dto.getMarketCap()
                    + " | 카테고리: " + dto.getCategoryName());
        }

        return list;
    }

}
