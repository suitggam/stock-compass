package com.stock.survive.serviceImpl;


import com.stock.survive.dto.StockInfosDto;
import com.stock.survive.entity.StockInfos;
import com.stock.survive.repository.StockInfosRepository;
import com.stock.survive.service.StockInfosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.stream;


@Service
@RequiredArgsConstructor
public class StockInfosServiceImpl implements StockInfosService {

    private final StockInfosRepository stockInfosRepository;

    @Override
    public List<StockInfosDto> getStock(String ticker) {
        List<StockInfos> infos = stockInfosRepository.findRecent6YearsByTicker(ticker);
        return infos.stream()
                .map(stockInfos -> StockInfosDto.builder()
                        .ticker(stockInfos.getStockItem().getTicker())
                        .companyName(stockInfos.getStockItem().getCompanyName())
                        .endPrice(stockInfos.getEndPrice())
                        .date(stockInfos.getDate())
                        .build())
                .toList();
    }

}
