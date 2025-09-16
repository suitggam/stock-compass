package com.stock.survive;

import com.stock.survive.entity.StockInfos;
import com.stock.survive.repository.StockInfosRepository;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@Log4j2
public class StockInfosTest {

    @Autowired
    StockInfosRepository stockInfosRepository;

    @Test
    public void repoTest() {
        Assertions.assertNotNull(stockInfosRepository);
        log.info(stockInfosRepository.getClass().getName());
    }

    @Test
    @Transactional
    public void getTest() {
        List<StockInfos> list = stockInfosRepository.findAll();
        for (StockInfos stockInfos : list) {
            log.info(stockInfos.getStockItem().getCompanyName()+", "+stockInfos.getStockItem().getTicker()+", "+stockInfos.getVolume()+", "+stockInfos.getMarketCap());
        }
    }

    @Test
    @Transactional
    public void getOneTest() {
        Long id = 1L;
        Optional<StockInfos> list = stockInfosRepository.findById(id);
        StockInfos stockInfos=list.orElseThrow();
            log.info(stockInfos.getStockItem().getCompanyName()+", "+stockInfos.getStockItem().getTicker()+", "+stockInfos.getVolume()+", "+stockInfos.getMarketCap());
    }

}