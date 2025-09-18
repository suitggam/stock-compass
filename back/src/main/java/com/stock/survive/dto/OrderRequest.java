package com.stock.survive.dto;

import com.stock.survive.entity.TradeHistory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull
    private Integer userNo;

    @NotNull
    private Integer stockNo;

    @NotNull
    private TradeHistory.TradeType type;

    @NotNull
    private TradeHistory.OrderType orderType;

    @Min(0)
    private Integer price;

    @NotNull
    @Min(1)
    private Integer volume;

    @Min(0)
    private Integer triggerPrice;

    private LocalDateTime expiresAt;
}
