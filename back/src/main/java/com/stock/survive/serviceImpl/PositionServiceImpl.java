package com.stock.survive.serviceImpl;

import com.stock.survive.entity.AccountHistory;
import com.stock.survive.entity.Position;
import com.stock.survive.entity.TradeHistory;
import com.stock.survive.entity.User;
import com.stock.survive.repository.AccountHistoryRepository;
import com.stock.survive.repository.PositionRepository;
import com.stock.survive.repository.UserRepository;
import com.stock.survive.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionServiceImpl implements PositionService {

    private final PositionRepository positionRepository;
    private final AccountHistoryRepository accountHistoryRepository;
    private final UserRepository userRepository;

    @Override
    public Optional<Position> getPosition(Integer userNo, Integer stockNo) {
        return positionRepository.findByUserNoAndStockNo(userNo, stockNo);
    }

    @Override
    public List<Position> getUserPositions(Integer userNo) {
        return positionRepository.findByUserNo(userNo);
    }

    @Override
    @Transactional
    public Position createOrUpdatePosition(Integer userNo, Integer stockNo, Integer quantity, BigDecimal price) {
        Optional<Position> existingPosition = positionRepository.findByUserNoAndStockNo(userNo, stockNo);
        
        if (existingPosition.isPresent()) {
            // 기존 포지션 업데이트
            Position position = existingPosition.get();
            int newQuantity = position.getStockCnt() + quantity;
            
            if (newQuantity <= 0) {
                // 수량이 0 이하면 포지션 삭제
                positionRepository.delete(position);
                log.info("포지션 삭제: userNo={}, stockNo={}", userNo, stockNo);
                return null;
            }
            
            // 평균 단가 계산
            BigDecimal newAvgPrice = calculateAveragePrice(position, quantity, price.intValue());
            position.updatePosition(newQuantity, newAvgPrice);
            
            Position savedPosition = positionRepository.save(position);
            log.info("포지션 업데이트: userNo={}, stockNo={}, quantity={}, avgPrice={}", 
                    userNo, stockNo, newQuantity, newAvgPrice);
            return savedPosition;
        } else {
            // 새 포지션 생성
            Position newPosition = Position.builder()
                    .userNo(userNo)
                    .stockNo(stockNo)
                    .stockCnt(quantity)
                    .unitPrice(price)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            Position savedPosition = positionRepository.save(newPosition);
            log.info("새 포지션 생성: userNo={}, stockNo={}, quantity={}, price={}", 
                    userNo, stockNo, quantity, price);
            return savedPosition;
        }
    }

    @Override
    public Integer getHoldingQuantity(Integer userNo, Integer stockNo) {
        Optional<Position> position = positionRepository.findByUserNoAndStockNo(userNo, stockNo);
        return position.map(Position::getStockCnt).orElse(0);
    }

    @Override
    public BigDecimal calculateAveragePrice(Position existingPosition, Integer newQuantity, Integer newPrice) {
        if (newQuantity == 0) {
            return existingPosition.getUnitPrice();
        }
        
        BigDecimal existingValue = existingPosition.getUnitPrice().multiply(BigDecimal.valueOf(existingPosition.getStockCnt()));
        BigDecimal newValue = BigDecimal.valueOf(newPrice).multiply(BigDecimal.valueOf(newQuantity));
        BigDecimal totalValue = existingValue.add(newValue);
        BigDecimal totalQuantity = BigDecimal.valueOf(existingPosition.getStockCnt() + newQuantity);
        
        return totalValue.divide(totalQuantity, 1, RoundingMode.HALF_UP);
    }

    // 추가 메서드들
    @Override
    @Transactional
    public void updatePosition(Integer userNo, Integer stockNo, Integer quantity, Integer price, TradeHistory.TradeType tradeType) {
        Position position = positionRepository.findByUserNoAndStockNo(userNo, stockNo)
                .orElse(Position.builder()
                        .userNo(userNo)
                        .stockNo(stockNo)
                        .stockCnt(0)
                        .unitPrice(BigDecimal.ZERO)
                        .build());

        if (tradeType == TradeHistory.TradeType.BUY) {
            // 매수: 수량 증가, 평균 단가 계산
            int newQuantity = position.getStockCnt() + quantity;
            BigDecimal newAvgPrice = calculateAveragePrice(position, quantity, price);
            position.updatePosition(newQuantity, newAvgPrice);
        } else { // SELL
            // 매도: 수량 감소
            int newQuantity = position.getStockCnt() - quantity;
            if (newQuantity < 0) {
                log.error("매도 수량이 보유 수량보다 많습니다. userNo={}, stockNo={}, currentCnt={}, sellCnt={}",
                        userNo, stockNo, position.getStockCnt(), quantity);
                throw new RuntimeException("보유 수량보다 많은 주식을 매도할 수 없습니다.");
            }
            position.updatePosition(newQuantity, position.getUnitPrice()); // 매도 시 평균 단가는 변동 없음
        }

        if (position.getStockCnt() == 0) {
            positionRepository.delete(position); // 보유 수량이 0이 되면 포지션 삭제
            log.info("포지션 삭제: userNo={}, stockNo={}", userNo, stockNo);
        } else {
            positionRepository.save(position);
            log.info("포지션 업데이트: userNo={}, stockNo={}, stockCnt={}, unitPrice={}",
                    userNo, stockNo, position.getStockCnt(), position.getUnitPrice());
        }
    }

    @Override
    public Long getCurrentCashBalance(Integer userNo) {
        // User 테이블의 초기 cash 값을 기준으로 AccountHistory의 최종 잔액을 조회
        return accountHistoryRepository.findLatestRemainByUserNo(userNo)
                .orElseGet(() -> userRepository.findById(userNo)
                        .map(User::getCash)
                        .map(Integer::longValue)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")));
    }

    @Override
    @Transactional
    public void updateAccountBalance(Integer userNo, Integer transactionValue) {
        Long currentBalance = getCurrentCashBalance(userNo);
        Long newBalance = currentBalance + transactionValue;

        AccountHistory accountHistory = AccountHistory.builder()
                .userNo(userNo)
                .transactionValue(transactionValue)
                .remain(newBalance)
                .createdAt(LocalDateTime.now())
                .build();
        accountHistoryRepository.save(accountHistory);
        log.info("계좌 잔고 업데이트: userNo={}, transactionValue={}, newBalance={}", userNo, transactionValue, newBalance);
    }
}
