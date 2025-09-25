package com.stock.survive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeCardDto {
//    private String tradeType; // "BUY" or "SELL"
    private Long price;
    private Integer volume;
//    private Long totalPrice;
}
