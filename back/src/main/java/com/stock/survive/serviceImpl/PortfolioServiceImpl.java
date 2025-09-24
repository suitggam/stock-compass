package com.stock.survive.serviceImpl;

import com.stock.survive.dto.HoldingDto;
import com.stock.survive.dto.PortfolioSummaryDto;
import com.stock.survive.entity.Position;
import com.stock.survive.entity.StockItems;
import com.stock.survive.repository.PositionRepository;
import com.stock.survive.repository.StockInfosRepository;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.repository.StockRealtimeRepository;
import com.stock.survive.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PositionRepository positionRepository;
    private final StockItemsRepository stockItemsRepository;
    private final StockRealtimeRepository stockRealtimeRepository;
    private final StockInfosRepository stockInfosRepository;

    @Override
    public PortfolioSummaryDto getHoldingsSummary(Integer userNo) {
        List<Position> positions = positionRepository.findByUserNo(userNo);
        List<HoldingDto> list = new ArrayList<>();

        long totalInvested = 0L;
        long totalMarket = 0L;

        for (Position p : positions) {
            Integer stockNo = p.getStockNo();
            StockItems item = stockItemsRepository.findById(stockNo).orElse(null);
            String ticker = item != null ? item.getTicker() : null;
            String companyName = item != null ? item.getCompanyName() : null;

            Integer currentPrice = null;
            if (ticker != null) {
                currentPrice = stockRealtimeRepository.findByTicker(ticker).map(r -> r.getPrice()).orElse(null);
            }
            if (currentPrice == null) {
                currentPrice = stockInfosRepository.findLatestEndPriceByItemNo(stockNo).orElse(0);
            }

            int qty = p.getStockCnt();
            BigDecimal avg = p.getUnitPrice() != null ? p.getUnitPrice() : BigDecimal.ZERO;
            long invested = avg.multiply(BigDecimal.valueOf(qty)).longValue();
            long marketValue = (long) (currentPrice != null ? currentPrice : 0) * qty;
            long pnl = marketValue - invested;
            double pnlRate = invested == 0 ? 0.0 : (pnl * 100.0 / invested);

            totalInvested += invested;
            totalMarket += marketValue;

            list.add(HoldingDto.builder()
                    .stockNo(stockNo)
                    .ticker(ticker)
                    .companyName(companyName)
                    .quantity(qty)
                    .avgPrice(avg)
                    .currentPrice(currentPrice)
                    .invested(invested)
                    .marketValue(marketValue)
                    .pnl(pnl)
                    .pnlRate(Math.round(pnlRate * 100.0) / 100.0)
                    .build());
        }

        long totalPnl = totalMarket - totalInvested;
        double totalRate = totalInvested == 0 ? 0.0 : (totalPnl * 100.0 / totalInvested);

        return PortfolioSummaryDto.builder()
                .holdings(list)
                .totalInvested(totalInvested)
                .totalMarketValue(totalMarket)
                .totalPnl(totalPnl)
                .totalPnlRate(Math.round(totalRate * 100.0) / 100.0)
                .build();
    }
}

