package com.stock.survive.service;

import com.stock.survive.dto.FavoriteDto;
import com.stock.survive.dto.PageRequestDto;
import com.stock.survive.dto.PageResponseDto;
import com.stock.survive.dto.StockEndDayDto;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Transactional
public interface StockItemsService {
    PageResponseDto<StockEndDayDto> getEndDayData(PageRequestDto pageRequestDto, LocalDate targetDate);

    LocalDate getLatestDataDate();

    FavoriteDto getFavoriteStatus(Long userId, String ticker);
}
