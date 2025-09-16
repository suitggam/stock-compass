package com.stock.survive.controller;

import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.service.StockItemsService;
import com.stock.survive.service.StockRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Log4j2
public class StockItemController {

    private final StockItemsService stockItemsService;
    // 장 마감 데이터 조회
    @GetMapping("/endDay")
    public ResponseEntity<List<StockEndDayDto>> getEndOfDayData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // date가 없으면 기본적으로 어제 날짜 사용
        LocalDate targetDate = (date != null) ? date : LocalDate.now().minusDays(1);
        List<StockEndDayDto> dtos = stockItemsService.getEndDayData(targetDate);
        return ResponseEntity.ok(dtos);
    }
}
