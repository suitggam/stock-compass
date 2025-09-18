package com.stock.survive.controller;

import com.stock.survive.dto.PageRequestDto;
import com.stock.survive.dto.PageResponseDto;
import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.service.StockItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Log4j2
public class StockItemsController {

    private final StockItemsService stockItemsService;

    // 장 마감 데이터 조회 (페이지네이션 적용)
    @GetMapping("/endDay")
    public ResponseEntity<PageResponseDto<StockEndDayDto>> getEndOfDayData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "21") int size // ✅ 기본값 20으로 변경
    ) {
        // date가 없으면 기본적으로 어제 날짜 사용
        LocalDate targetDate = (date != null) ? date : LocalDate.now().minusDays(1);

        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .page(page)
                .size(size)
                .build();

        PageResponseDto<StockEndDayDto> response = stockItemsService.getEndDayData(pageRequestDto, targetDate);
        return ResponseEntity.ok(response);
    }
}
