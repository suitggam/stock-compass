package com.stock.survive.controller;


import com.stock.survive.dto.StockInfosDto;
import com.stock.survive.service.StockInfosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Log4j2
public class StockInfosController {

    private final StockInfosService stockInfosService;

    @GetMapping("/info/{ticker}")
    public ResponseEntity<List<StockInfosDto>> getStockInfo(@PathVariable("ticker") String ticker) {
        List<StockInfosDto> response = stockInfosService.getStock(ticker);
        return ResponseEntity.ok(response);
    }
}
