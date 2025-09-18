package com.stock.survive.scheduler;

import com.stock.survive.service.OrderService;
import com.stock.survive.service.TradingHoursService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

    private final OrderService orderService;
    private final TradingHoursService tradingHoursService;

    // 1초마다 대기 주문 처리
    @Scheduled(fixedRate = 1000)
    public void processPendingOrders() {
        if (tradingHoursService.isMarketOpen()) {
            orderService.processPendingOrders();
        }
    }

    // 매일 15:30에 만료된 주문 정리
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void expireOrdersAtMarketClose() {
        log.info("장 마감 - 만료된 주문들을 정리합니다");
        orderService.expireOrders();
    }

    // 매일 08:50에 시스템 준비
    @Scheduled(cron = "0 50 8 * * MON-FRI")
    public void prepareForMarketOpen() {
        log.info("장 시작 준비 - 시스템 상태를 점검합니다");
        // 시스템 상태 점검 로직
    }
}
