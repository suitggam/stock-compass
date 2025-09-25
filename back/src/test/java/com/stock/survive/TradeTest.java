package com.stock.survive;

import com.stock.survive.dto.TradeHistoryDto;
import com.stock.survive.entity.User;
import com.stock.survive.repository.UserRepository;
import com.stock.survive.service.TradeHistoryService;
import com.stock.survive.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@SpringBootTest
@Log4j2
public class TradeTest {
    @Autowired
    TradeHistoryService tradeHistoryService;

    @Autowired
    UserRepository userRepository;


    @Test
    void buyTest() {

        Long userNo = 1L;
        String ticker = "005930";

        Long price = 83000L;
        int volume = 3;

        TradeHistoryDto tradeHistoryDto = tradeHistoryService.processBuy(userNo, ticker, price, volume);

        log.info(tradeHistoryDto.getTradeType());
        log.info(tradeHistoryDto.getPrice());
        log.info(tradeHistoryDto.getVolume());
        log.info(tradeHistoryDto.getTotalPrice());
        log.info(tradeHistoryDto.getCreateAt());
        log.info("---------------------------");

        Optional<User> optionalUser=userRepository.findById(userNo);
        log.info(optionalUser.get().getCash());
        log.info(optionalUser.get().getHaveStock());

    }


}
