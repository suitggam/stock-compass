package com.stock.survive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryDto {
    private List<HoldingDto> holdings;
    private Long totalInvested;
    private Long totalMarketValue;
    private Long totalPnl;
    private Double totalPnlRate;
}

