package com.stock.survive.service;

import com.stock.survive.dto.PageRequestDto;
import com.stock.survive.dto.PageResponseDto;
import com.stock.survive.dto.StockRealtimeDto;
import jakarta.transaction.Transactional;

@Transactional
public interface StockRealtimeService {

    PageResponseDto<StockRealtimeDto> getList(PageRequestDto pageRequestDto);
    
    // 실시간 주가 업데이트 (이벤트 발행)
    void updateStockPrice(String ticker, Integer newPrice);
    
    // 현재 주가 조회
    Integer getCurrentPrice(String ticker);

}