package com.stock.survive.serviceImpl;

import com.stock.survive.entity.AccountHistory;
import com.stock.survive.entity.TradeHistory;
import com.stock.survive.repository.AccountHistoryRepository;
import com.stock.survive.repository.TradeHistoryRepository;
import com.stock.survive.service.ExecutionService;
import com.stock.survive.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionServiceImpl implements ExecutionService {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final AccountHistoryRepository accountHistoryRepository;
    private final PositionService positionService;

    @Override
    public boolean canExecuteLimitOrder(TradeHistory order, Integer currentPrice) {
        if (order.getType() == TradeHistory.TradeType.BUY) {
            // 매수: 주문가 >= 현재가
            return order.getPrice() >= currentPrice;
        } else {
            // 매도: 주문가 <= 현재가
            return order.getPrice() <= currentPrice;
        }
    }

    @Override
    public boolean shouldTriggerConditionalOrder(TradeHistory order, Integer currentPrice) {
        if (order.getOrderType() == TradeHistory.OrderType.STOP_LOSS) {
            // 손절: 현재가 <= 손절가
            return currentPrice <= order.getTriggerPrice();
        } else if (order.getOrderType() == TradeHistory.OrderType.TAKE_PROFIT) {
            // 익절: 현재가 >= 익절가
            return currentPrice >= order.getTriggerPrice();
        }
        return false;
    }

    @Override
    @Transactional
    public void executeOrder(TradeHistory order, Integer executionPrice) {
        log.info("주문 체결: tradeNo={}, executionPrice={}", order.getTradeNo(), executionPrice);
        
        // 주문 상태를 FILLED로 업데이트
        TradeHistory executedOrder = TradeHistory.builder()
                .tradeNo(order.getTradeNo())
                .userNo(order.getUserNo())
                .stockNo(order.getStockNo())
                .type(order.getType())
                .orderType(order.getOrderType())
                .price(executionPrice)
                .volume(order.getVolume())
                .status(TradeHistory.OrderStatus.FILLED)
                .triggerPrice(order.getTriggerPrice())
                .expiresAt(order.getExpiresAt())
                .createdAt(order.getCreatedAt())
                .build();
        
        tradeHistoryRepository.save(executedOrder);
        
        // 포지션 업데이트
        updatePosition(order.getUserNo(), order.getStockNo(), order.getVolume(), executionPrice, order.getType());
        
        // 계좌 잔고 업데이트
        int amount = order.getType() == TradeHistory.TradeType.BUY 
                ? -executionPrice * order.getVolume()  // 매수시 차감
                : executionPrice * order.getVolume();   // 매도시 증가
        
        updateAccountBalance(order.getUserNo(), amount);
        
        log.info("주문 체결 완료: tradeNo={}, amount={}", order.getTradeNo(), amount);
    }

    @Override
    @Transactional
    public void executeMarketOrder(TradeHistory order, Integer currentPrice) {
        // 슬리피지 적용 (±0.1% 랜덤 변동)
        double slippage = (Math.random() - 0.5) * 0.002; // ±0.1%
        int executionPrice = (int) (currentPrice * (1 + slippage));
        
        log.info("시장가 주문 체결: tradeNo={}, currentPrice={}, executionPrice={}", 
                order.getTradeNo(), currentPrice, executionPrice);
        
        executeOrder(order, executionPrice);
    }

    @Override
    @Transactional
    public void updatePosition(Integer userNo, Integer stockNo, Integer quantity, Integer price, TradeHistory.TradeType type) {
        positionService.createOrUpdatePosition(userNo, stockNo, quantity, BigDecimal.valueOf(price));
        log.info("포지션 업데이트: userNo={}, stockNo={}, quantity={}, price={}, type={}", 
                userNo, stockNo, quantity, price, type);
    }

    @Override
    @Transactional
    public void updateAccountBalance(Integer userNo, Integer amount) {
        // 현재 잔고 조회
        Long currentBalance = getCurrentBalance(userNo);
        Long newBalance = currentBalance + amount;
        
        // 계좌 내역 추가
        AccountHistory history = AccountHistory.builder()
                .userNo(userNo)
                .transactionValue(amount)
                .remain(newBalance)
                .createdAt(LocalDateTime.now())
                .build();
        
        accountHistoryRepository.save(history);
        log.info("계좌 잔고 업데이트: userNo={}, amount={}, newBalance={}", userNo, amount, newBalance);
    }

    @Override
    @Transactional
    public void processPendingOrdersForStock(Integer stockNo, Integer currentPrice) {
        // 지정가 주문들 처리
        List<TradeHistory> limitOrders = tradeHistoryRepository.findPendingLimitOrdersByStock(stockNo);
        for (TradeHistory order : limitOrders) {
            if (canExecuteLimitOrder(order, currentPrice)) {
                executeOrder(order, currentPrice);
            }
        }
        
        // 조건부 주문들 처리
        List<TradeHistory> conditionalOrders = tradeHistoryRepository.findPendingConditionalOrdersByStock(stockNo);
        for (TradeHistory order : conditionalOrders) {
            if (shouldTriggerConditionalOrder(order, currentPrice)) {
                executeOrder(order, currentPrice);
            }
        }
    }

    private Long getCurrentBalance(Integer userNo) {
        List<AccountHistory> histories = accountHistoryRepository.findLatestByUser(userNo);
        if (histories.isEmpty()) {
            return 10_000_000L; // 초기 자금
        }
        return histories.get(0).getRemain();
    }
}
