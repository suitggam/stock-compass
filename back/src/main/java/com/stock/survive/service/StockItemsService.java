package com.stock.survive.service;

import com.stock.survive.dto.PageRequestDto;
import com.stock.survive.dto.PageResponseDto;
import com.stock.survive.dto.StockEndDayDto;
import jakarta.transaction.Transactional;

import java.time.LocalDate;

@Transactional
public interface StockItemsService {
    PageResponseDto<StockEndDayDto> getEndDayData(PageRequestDto pageRequestDto, LocalDate targetDate);

    LocalDate getLatestDataDate();
}
