package com.stock.survive.controller;

import com.stock.survive.dto.PageRequestDto;
import com.stock.survive.dto.PageResponseDto;
import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.service.StockItemsService;
import com.stock.survive.service.StockQueryService;
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
import com.stock.survive.dto.StockItemOptionDto;
import com.stock.survive.dto.StockPricePointDto;
import com.stock.survive.dto.StockCandlePointDto;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Log4j2
public class StockItemsController {

    private final StockItemsService stockItemsService;
    private final StockQueryService stockQueryService;

    // 장 마감 데이터 조회 (페이지네이션 적용)
    @GetMapping("/endDay")
    public ResponseEntity<PageResponseDto<StockEndDayDto>> getEndOfDayData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "21") int size
    ) {
        // date가 null이면 DB에서 가장 최신 날짜 사용
        LocalDate targetDate = (date != null) ? date : stockItemsService.getLatestDataDate();

        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .page(page)
                .size(size)
                .build();

        PageResponseDto<StockEndDayDto> response = stockItemsService.getEndDayData(pageRequestDto, targetDate);
        log.info("targetDate = " + targetDate + ", totalCount = " + response.getTotalCount());
        return ResponseEntity.ok(response);
    }


    // 종목 전체 목록 (itemNo/ticker/companyName)
    @GetMapping("/items")
    public ResponseEntity<List<StockItemOptionDto>> listItems() {
        return ResponseEntity.ok(stockQueryService.listItems());
    }

    // 특정 종목의 기간별 일봉(endPrice) 히스토리
    @GetMapping("/history")
    public ResponseEntity<List<StockPricePointDto>> getHistory(
            @RequestParam Integer itemNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate toDate = (to != null) ? to : LocalDate.now();
        LocalDate fromDate = (from != null) ? from : toDate.minusYears(5);
        return ResponseEntity.ok(stockQueryService.getHistory(itemNo, fromDate, toDate));
    }

    // 캔들(OHLC) + 거래량
    @GetMapping("/history/candle")
    public ResponseEntity<List<StockCandlePointDto>> getCandleHistory(
            @RequestParam Integer itemNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate toDate = (to != null) ? to : LocalDate.now();
        LocalDate fromDate = (from != null) ? from : toDate.minusYears(5);
        return ResponseEntity.ok(stockQueryService.getCandleHistory(itemNo, fromDate, toDate));
    }
}
