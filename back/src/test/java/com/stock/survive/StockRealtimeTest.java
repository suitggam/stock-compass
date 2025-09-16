package com.stock.survive;

import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.dto.StockRealtimeDto;
import com.stock.survive.entity.StockRealtime;
import com.stock.survive.repository.StockRealtimeRepository;
import com.stock.survive.service.StockRealtimeService;
import com.stock.survive.serviceImp.StockRealtimeServiceImp;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@Log4j2
public class StockRealtimeTest {

    @Autowired
    StockRealtimeRepository stockRealtimeRepository;
    @Autowired
    StockRealtimeService stockRealtimeService;

    @Test
    public void repoTest() {
        Assertions.assertNotNull(stockRealtimeRepository);
        log.info(stockRealtimeRepository.getClass().getName());
    }

    @Test
    @Transactional
    public void getTest() {
        List<StockRealtime> list = stockRealtimeRepository.findAll();
        for (StockRealtime realtime : list) {
            log.info(realtime.getTicker() + ", " + realtime.getCompanyName() + ", " + realtime.getPrice() + ", " + realtime.getRate());
        }

    }

    @Test
    @Transactional
    public void getOneTest() {
        Long id = 1L;
        Optional<StockRealtime> list = stockRealtimeRepository.findById(id);
        StockRealtime realtime = list.orElseThrow();
        log.info(realtime.getTicker() + ", " + realtime.getCompanyName() + ", " + realtime.getPrice() + ", " + realtime.getRate());
    }

    @Test
    @Transactional
    public void getStockItemsDTOTest() {
        List<StockRealtimeDto> dtoList = stockRealtimeRepository.getAllStockRealtimeWithLatestInfo();

        for (StockRealtimeDto dto : dtoList) {
            log.info(dto.getTicker()
                    + ", " + dto.getVolume()
                    + ", " + dto.getMarketCap()
                    + ", " + dto.getCategoryName());
        }

        Assertions.assertFalse(dtoList.isEmpty(), "DTO 리스트가 비어있으면 안됨");
    }




}