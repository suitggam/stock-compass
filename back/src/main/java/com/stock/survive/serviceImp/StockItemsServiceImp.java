package com.stock.survive.serviceImp;

import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.service.StockItemsService;
import com.stock.survive.service.StockRealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockItemsServiceImp implements StockItemsService {

    private final StockItemsRepository stockItemsRepository;


    @Override
    public List<StockEndDayDto> getEndDayData(LocalDate targetDate) {
        List<StockEndDayDto> dtos = stockItemsRepository.getEndOfDayData(targetDate);

        dtos.forEach(dto -> {
            if (dto.getStartPrice() != null && dto.getStartPrice() != 0 && dto.getEndPrice() != null) {
                double rate = ((dto.getEndPrice() - dto.getStartPrice()) * 100.0 / dto.getStartPrice());
                dto.setRate(Math.round(rate * 100.0) / 100.0); // ✅ 소수점 둘째자리까지 반올림
            }
        });

        return dtos;
    }

}
