package com.stock.survive.serviceImp;

import com.stock.survive.dto.StockItemOptionDto;
import com.stock.survive.dto.StockPricePointDto;
import com.stock.survive.dto.StockCandlePointDto;
import com.stock.survive.repository.StockInfosRepository;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockQueryServiceImpl implements StockQueryService {

    private final StockItemsRepository stockItemsRepository;
    private final StockInfosRepository stockInfosRepository;

    @Override
    public List<StockItemOptionDto> listItems() {
        return stockItemsRepository.findAllOptions();
    }

    @Override
    public List<StockPricePointDto> getHistory(Integer itemNo, LocalDate from, LocalDate to) {
        var list = stockInfosRepository.findHistory(itemNo, from, to);
        if (list != null && !list.isEmpty()) return list;

        // fallback: stock_data table (ticker-based)
        var rows = stockItemsRepository.findHistoryFromStockData(itemNo, from, to);
        return rows.stream()
                .map(v -> new StockPricePointDto(v.getDate(), v.getEndPrice()))
                .toList();
    }

    @Override
    public List<StockCandlePointDto> getCandleHistory(Integer itemNo, LocalDate from, LocalDate to) {
        var list = stockInfosRepository.findCandleHistory(itemNo, from, to);
        if (list != null && !list.isEmpty()) return list;

        var rows = stockItemsRepository.findCandleFromStockData(itemNo, from, to);
        return rows.stream()
                .map(v -> new StockCandlePointDto(v.getDate(), v.getOpen(), v.getHigh(), v.getLow(), v.getClose(), v.getVolume()))
                .toList();
    }
}
