package com.stock.survive.schedule;

import com.stock.survive.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 30000)
    public void expirePendingOrders() {
        try {
            orderService.expireOrders();
        } catch (Exception e) {
            log.error("expire orders error: {}", e.getMessage());
        }
    }
}

