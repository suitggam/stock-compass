package com.stock.survive.event;

import com.stock.survive.service.ExecutionService;
import com.stock.survive.service.TradingHoursService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ExecutionService executionService;
    private final TradingHoursService tradingHoursService;

    @EventListener
    public void handleStockPriceChange(StockPriceChangeEvent event) {
        log.info("주가 변경 감지: ticker={}, oldPrice={}, newPrice={}", 
                event.getTicker(), event.getOldPrice(), event.getNewPrice());
        
        // 거래 시간이 아닌 경우 처리하지 않음
        if (!tradingHoursService.isTradingTime()) {
            log.debug("거래 시간이 아니므로 주문 처리를 건너뜁니다");
            return;
        }
        
        try {
            // 해당 종목의 대기 주문들 처리
            executionService.processPendingOrdersForStock(event.getItemNo(), event.getNewPrice());
            log.info("주문 처리 완료: stockNo={}, price={}", event.getItemNo(), event.getNewPrice());
        } catch (Exception e) {
            log.error("주문 처리 중 오류 발생: stockNo={}, price={}, error={}", 
                    event.getItemNo(), event.getNewPrice(), e.getMessage(), e);
        }
    }
}
