package com.stock.survive.service;

import com.stock.survive.entity.TradeHistory;

public interface ExecutionService {
    
    // 지정가 주문 체결 조건 확인
    boolean canExecuteLimitOrder(TradeHistory order, Integer currentPrice);
    
    // 조건부 주문 트리거 확인
    boolean shouldTriggerConditionalOrder(TradeHistory order, Integer currentPrice);
    
    // 주문 체결 처리
    void executeOrder(TradeHistory order, Integer executionPrice);
    
    // 시장가 주문 즉시 체결
    void executeMarketOrder(TradeHistory order, Integer currentPrice);
    
    // 포지션 업데이트
    void updatePosition(Integer userNo, Integer stockNo, Integer quantity, Integer price, TradeHistory.TradeType type);
    
    // 계좌 잔고 업데이트
    void updateAccountBalance(Integer userNo, Integer amount);
    
    // 특정 종목의 대기 주문들 처리
    void processPendingOrdersForStock(Integer stockNo, Integer currentPrice);
}
