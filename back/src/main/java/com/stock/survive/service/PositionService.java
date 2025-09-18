package com.stock.survive.service;

import com.stock.survive.entity.Position;
import com.stock.survive.entity.TradeHistory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PositionService {
    
    // 사용자의 특정 종목 포지션 조회
    Optional<Position> getPosition(Integer userNo, Integer stockNo);
    
    // 사용자의 모든 포지션 조회
    List<Position> getUserPositions(Integer userNo);
    
    // 포지션 생성 또는 업데이트
    Position createOrUpdatePosition(Integer userNo, Integer stockNo, Integer quantity, BigDecimal price);
    
    // 포지션 수량 확인
    Integer getHoldingQuantity(Integer userNo, Integer stockNo);
    
    // 포지션 평균 단가 계산
    BigDecimal calculateAveragePrice(Position existingPosition, Integer newQuantity, Integer newPrice);
    
    // 포지션 업데이트 (거래 타입별)
    @Transactional
    void updatePosition(Integer userNo, Integer stockNo, Integer quantity, Integer price, TradeHistory.TradeType tradeType);
    
    // 사용자 현재 현금 잔고 조회
    Long getCurrentCashBalance(Integer userNo);
    
    // 계좌 잔고 업데이트
    @Transactional
    void updateAccountBalance(Integer userNo, Integer transactionValue);
}
