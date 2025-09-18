package com.stock.survive.schedule;

import com.stock.survive.entity.StockItems;
import com.stock.survive.entity.StockRealtime;
import com.stock.survive.entity.TradeHistory;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.repository.StockRealtimeRepository;
import com.stock.survive.repository.TradeHistoryRepository;
import com.stock.survive.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PricePoller {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final StockItemsRepository stockItemsRepository;
    private final StockRealtimeRepository stockRealtimeRepository;
    private final ExecutionService executionService;

    private final Map<Integer, Integer> lastPriceByStock = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 1000)
    public void pollAndProcess() {
        try {
            var pending = tradeHistoryRepository.findByStatus(TradeHistory.OrderStatus.PENDING);
            if (pending.isEmpty()) return;

            Set<Integer> stockNos = pending.stream()
                    .map(TradeHistory::getStockNo)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (Integer stockNo : stockNos) {
                try {
                    Optional<StockItems> itemOpt = stockItemsRepository.findById(stockNo);
                    if (itemOpt.isEmpty()) continue;
                    String ticker = itemOpt.get().getTicker();
                    Optional<StockRealtime> rtOpt = stockRealtimeRepository.findByTicker(ticker);
                    if (rtOpt.isEmpty()) continue;
                    Integer currentPrice = rtOpt.get().getPrice();
                    if (currentPrice == null) continue;

                    Integer prev = lastPriceByStock.put(stockNo, currentPrice);
                    if (prev == null || !prev.equals(currentPrice)) {
                        executionService.processPendingOrdersForStock(stockNo, currentPrice);
                    }
                } catch (Exception e) {
                    log.error("poll/process error: stockNo={}, msg={}", stockNo, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("price poller error: {}", e.getMessage());
        }
    }
}

