package com.stock.survive.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "trade_histories")
public class TradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_no")
    private Long tradeNo;

    @Column(name = "stock_no", nullable = false)
    private Long stockNo;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TradeType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "volume", nullable = false)
    private Integer volume;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "trigger_price")
    private Integer triggerPrice;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum TradeType {
        BUY, SELL
    }

    public enum OrderType {
        MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT, TIME_LIMIT
    }

    public enum OrderStatus {
        PENDING, PARTIAL_FILLED, FILLED, CANCELLED, EXPIRED
    }
}
