package com.stock.survive.serviceImp;

import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.dto.StockRealtimeDto;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.repository.StockRealtimeRepository;
import com.stock.survive.service.StockRealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
public class StockRealtimeServiceImp implements StockRealtimeService {


    private final StockRealtimeRepository stockRealtimeRepository;

    @Override
    public List<StockRealtimeDto> getAllStockRealtimeWithLatestInfo() {
        return stockRealtimeRepository.getAllStockRealtimeWithLatestInfo();
    }



}
