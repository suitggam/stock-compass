package com.stock.survive.service;

import com.stock.survive.dto.PortfolioSummaryDto;

public interface PortfolioService {
    PortfolioSummaryDto getHoldingsSummary(Integer userNo);
}

