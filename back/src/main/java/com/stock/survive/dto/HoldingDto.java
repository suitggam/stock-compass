package com.stock.survive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingDto {
    private Integer stockNo;
    private String ticker;
    private String companyName;

    private Integer quantity;
    private BigDecimal avgPrice; // 평균단가 (positions.unit_price)
    private Integer currentPrice; // 현재가

    private Long invested;    // avgPrice * qty
    private Long marketValue; // currentPrice * qty
    private Long pnl;         // marketValue - invested
    private Double pnlRate;   // pnl / invested * 100
}

