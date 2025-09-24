package com.stock.survive;

import com.stock.survive.entity.StockItems;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.service.StockItemsService;
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
public class StockItemsTest {

    @Autowired
    StockItemsRepository stockItemsRepository;

    @Autowired
    StockItemsService stockItemsService;

    @Test
    public void repoTest() {
        Assertions.assertNotNull(stockItemsRepository);
        log.info(stockItemsRepository.getClass().getName());
    }

    @Test
    @Transactional
    public void getTest() {
        List<StockItems> list = stockItemsRepository.findAll();
        for (StockItems stockItems : list) {
            log.info(stockItems.getItemNo() + ", " + stockItems.getCategory().getCategoryName() + ", " + stockItems.getTicker() + ", " + stockItems.getCompanyName());
        }
    }

    @Test
    @Transactional
    public void getOneTest() {
        Long id = 1L;
        Optional<StockItems> list = stockItemsRepository.findById(id);
        StockItems items = list.orElseThrow();
        log.info(items.getItemNo() + ", " + items.getCategory().getCategoryName() + ", " + items.getTicker() + ", " + items.getCompanyName());
    }

    @Test
    @Transactional
    public void getInsertTest() {
        // Correctly commented out
        // StockItems save = stockItemsRepository.save(new StockItems(100,1,"test","test",1));
        // log.info(save);
    }
}