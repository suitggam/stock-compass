package com.stock.survive.service;

import com.stock.survive.dto.StockEndDayDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public interface StockItemsService {

    List<StockEndDayDto> getEndDayData(LocalDate targetDate);
}
