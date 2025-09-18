package com.stock.survive.dto;

import com.stock.survive.entity.TradeHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private Long tradeNo;
    private Integer userNo;
    private Integer stockNo;
    private TradeHistory.TradeType type;
    private TradeHistory.OrderType orderType;
    private Integer price;
    private Integer volume;
    private TradeHistory.OrderStatus status;
    private Integer triggerPrice;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String message;
    private boolean success;
}
