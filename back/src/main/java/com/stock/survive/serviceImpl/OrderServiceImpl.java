package com.stock.survive.serviceImpl;

import com.stock.survive.dto.OrderRequest;
import com.stock.survive.dto.OrderResponse;
import com.stock.survive.entity.TradeHistory;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.repository.StockRealtimeRepository;
import com.stock.survive.repository.TradeHistoryRepository;
import com.stock.survive.service.ExecutionService;
import com.stock.survive.service.OrderService;
import com.stock.survive.service.PositionService;
import com.stock.survive.service.TradingHoursService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final TradingHoursService tradingHoursService;
    private final ExecutionService executionService;
    private final PositionService positionService;
    private final StockItemsRepository stockItemsRepository;
    private final StockRealtimeRepository stockRealtimeRepository;

    @Value("${app.trading.enforce:true}")
    private boolean enforceTradingHours;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // Basic validation
        try {
            if (request.getOrderType() == TradeHistory.OrderType.LIMIT && request.getPrice() == null) {
                return OrderResponse.builder().success(false).message("Price is required for LIMIT order").build();
            }
            if ((request.getOrderType() == TradeHistory.OrderType.STOP_LOSS || request.getOrderType() == TradeHistory.OrderType.TAKE_PROFIT)
                    && request.getTriggerPrice() == null) {
                return OrderResponse.builder().success(false).message("Trigger price is required for conditional order").build();
            }
            if (request.getOrderType() == TradeHistory.OrderType.TIME_LIMIT && request.getExpiresAt() == null) {
                return OrderResponse.builder().success(false).message("ExpiresAt is required for TIME_LIMIT order").build();
            }

            // Holdings check for SELL
            if (request.getType() == TradeHistory.TradeType.SELL
                    && (request.getOrderType() == TradeHistory.OrderType.MARKET || request.getOrderType() == TradeHistory.OrderType.LIMIT)) {
                Integer holding = positionService.getHoldingQuantity(request.getUserNo(), request.getStockNo());
                if (holding < request.getVolume()) {
                    return OrderResponse.builder().success(false).message("Insufficient holdings").build();
                }
            }

            // Cash check for BUY
            if (request.getType() == TradeHistory.TradeType.BUY) {
                Long cash = positionService.getCurrentCashBalance(request.getUserNo());
                Integer basisPrice = request.getPrice();
                if (request.getOrderType() == TradeHistory.OrderType.MARKET) {
                    var itemOpt = stockItemsRepository.findById(request.getStockNo());
                    if (itemOpt.isPresent()) {
                        var ticker = itemOpt.get().getTicker();
                        basisPrice = stockRealtimeRepository.findByTicker(ticker).map(r -> r.getPrice()).orElse(basisPrice);
                    }
                }
                if (basisPrice == null) basisPrice = 0;
                long cost = (long) basisPrice * request.getVolume();
                if (cash != null && cash < cost) {
                    return OrderResponse.builder().success(false).message("Insufficient cash balance").build();
                }
            }
        } catch (Exception e) {
            log.warn("order validation skipped: {}", e.getMessage());
        }

        // Trading hours check (can be disabled by flag)
        if (enforceTradingHours && !tradingHoursService.isTradingTime()) {
            return OrderResponse.builder()
                    .success(false)
                    .message("Market is closed (09:00-15:30)")
                    .build();
        }

        // Persist order as PENDING
        TradeHistory order = TradeHistory.builder()
                .userNo(request.getUserNo())
                .stockNo(request.getStockNo())
                .type(request.getType())
                .orderType(request.getOrderType())
                .price(request.getPrice())
                .volume(request.getVolume())
                .status(TradeHistory.OrderStatus.PENDING)
                .triggerPrice(request.getTriggerPrice())
                .expiresAt(request.getExpiresAt())
                .build();

        TradeHistory savedOrder = tradeHistoryRepository.save(order);

        // Execute market orders immediately
        if (savedOrder.getOrderType() == TradeHistory.OrderType.MARKET) {
            try {
                var itemOpt = stockItemsRepository.findById(savedOrder.getStockNo());
                Integer currentPrice = null;
                if (itemOpt.isPresent()) {
                    var ticker = itemOpt.get().getTicker();
                    currentPrice = stockRealtimeRepository.findByTicker(ticker).map(r -> r.getPrice()).orElse(null);
                }
                if (currentPrice == null) currentPrice = savedOrder.getPrice();
                if (currentPrice != null) {
                    executionService.executeMarketOrder(savedOrder, currentPrice);
                }
            } catch (Exception e) {
                log.error("market execute error: tradeNo={}, msg={}", savedOrder.getTradeNo(), e.getMessage());
            }
        }

        return OrderResponse.builder()
                .tradeNo(savedOrder.getTradeNo())
                .userNo(savedOrder.getUserNo())
                .stockNo(savedOrder.getStockNo())
                .type(savedOrder.getType())
                .orderType(savedOrder.getOrderType())
                .price(savedOrder.getPrice())
                .volume(savedOrder.getVolume())
                .status(savedOrder.getStatus())
                .triggerPrice(savedOrder.getTriggerPrice())
                .expiresAt(savedOrder.getExpiresAt())
                .createdAt(savedOrder.getCreatedAt())
                .success(true)
                .message("Order created")
                .build();
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long tradeNo, Integer userNo) {
        TradeHistory order = tradeHistoryRepository.findById(tradeNo)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUserNo().equals(userNo)) {
            throw new RuntimeException("Cannot cancel others' order");
        }

        if (order.getStatus() != TradeHistory.OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be cancelled");
        }

        order = TradeHistory.builder()
                .tradeNo(order.getTradeNo())
                .userNo(order.getUserNo())
                .stockNo(order.getStockNo())
                .type(order.getType())
                .orderType(order.getOrderType())
                .price(order.getPrice())
                .volume(order.getVolume())
                .status(TradeHistory.OrderStatus.CANCELLED)
                .triggerPrice(order.getTriggerPrice())
                .expiresAt(order.getExpiresAt())
                .createdAt(order.getCreatedAt())
                .build();

        TradeHistory savedOrder = tradeHistoryRepository.save(order);

        return OrderResponse.builder()
                .tradeNo(savedOrder.getTradeNo())
                .userNo(savedOrder.getUserNo())
                .stockNo(savedOrder.getStockNo())
                .type(savedOrder.getType())
                .orderType(savedOrder.getOrderType())
                .price(savedOrder.getPrice())
                .volume(savedOrder.getVolume())
                .status(savedOrder.getStatus())
                .triggerPrice(savedOrder.getTriggerPrice())
                .expiresAt(savedOrder.getExpiresAt())
                .createdAt(savedOrder.getCreatedAt())
                .success(true)
                .message("Order cancelled")
                .build();
    }

    @Override
    public List<OrderResponse> getUserOrders(Integer userNo) {
        List<TradeHistory> orders = tradeHistoryRepository.findByUserNoAndStatus(userNo, TradeHistory.OrderStatus.PENDING);
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getPendingOrders() {
        List<TradeHistory> orders = tradeHistoryRepository.findByStatus(TradeHistory.OrderStatus.PENDING);
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrder(Long tradeNo) {
        TradeHistory order = tradeHistoryRepository.findById(tradeNo)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return convertToResponse(order);
    }

    @Override
    @Transactional
    public void processPendingOrders() {
        log.info("Processing pending orders");
        List<TradeHistory> pendingOrders = tradeHistoryRepository.findByStatus(TradeHistory.OrderStatus.PENDING);
        for (TradeHistory order : pendingOrders) {
            try {
                if (order.getOrderType() == TradeHistory.OrderType.MARKET) {
                    Integer currentPrice = order.getPrice();
                    // executionService.executeMarketOrder(order, currentPrice);
                }
            } catch (Exception e) {
                log.error("Error processing order {}: {}", order.getTradeNo(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void expireOrders() {
        List<TradeHistory> expiredOrders = tradeHistoryRepository.findExpiredOrders(LocalDateTime.now());
        for (TradeHistory order : expiredOrders) {
            TradeHistory expiredOrder = TradeHistory.builder()
                    .tradeNo(order.getTradeNo())
                    .userNo(order.getUserNo())
                    .stockNo(order.getStockNo())
                    .type(order.getType())
                    .orderType(order.getOrderType())
                    .price(order.getPrice())
                    .volume(order.getVolume())
                    .status(TradeHistory.OrderStatus.EXPIRED)
                    .triggerPrice(order.getTriggerPrice())
                    .expiresAt(order.getExpiresAt())
                    .createdAt(order.getCreatedAt())
                    .build();
            tradeHistoryRepository.save(expiredOrder);
        }
        log.info("Expired orders processed: {}", expiredOrders.size());
    }

    private OrderResponse convertToResponse(TradeHistory order) {
        return OrderResponse.builder()
                .tradeNo(order.getTradeNo())
                .userNo(order.getUserNo())
                .stockNo(order.getStockNo())
                .type(order.getType())
                .orderType(order.getOrderType())
                .price(order.getPrice())
                .volume(order.getVolume())
                .status(order.getStatus())
                .triggerPrice(order.getTriggerPrice())
                .expiresAt(order.getExpiresAt())
                .createdAt(order.getCreatedAt())
                .success(true)
                .build();
    }
}

