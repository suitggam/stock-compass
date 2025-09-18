package com.stock.survive.service;

import com.stock.survive.dto.StockItemOptionDto;
import com.stock.survive.dto.StockPricePointDto;
import com.stock.survive.dto.StockCandlePointDto;

import java.time.LocalDate;
import java.util.List;

public interface StockQueryService {

    List<StockItemOptionDto> listItems();

    List<StockPricePointDto> getHistory(Integer itemNo, LocalDate from, LocalDate to);

    List<StockCandlePointDto> getCandleHistory(Integer itemNo, LocalDate from, LocalDate to);
}
