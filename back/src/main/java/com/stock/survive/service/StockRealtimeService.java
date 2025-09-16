package com.stock.survive.service;

import com.stock.survive.dto.StockRealtimeDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StockRealtimeService {
    List<StockRealtimeDto> getAllStockRealtimeWithLatestInfo();

}