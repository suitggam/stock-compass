package com.stock.survive.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_no")
    private Long positionNo;

    @Column(name = "user_no", nullable = false)
    private Integer userNo;

    @Column(name = "stock_no", nullable = false)
    private Integer stockNo;

    @Column(name = "unit_price", nullable = false, precision = 8, scale = 1)
    private BigDecimal unitPrice;

    @Column(name = "stock_cnt", nullable = false)
    private Integer stockCnt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updatePosition(Integer newStockCnt, BigDecimal newUnitPrice) {
        this.stockCnt = newStockCnt;
        this.unitPrice = newUnitPrice;
        this.updatedAt = LocalDateTime.now();
    }
}
