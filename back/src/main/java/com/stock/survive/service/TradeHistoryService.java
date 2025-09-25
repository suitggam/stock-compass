package com.stock.survive.service;

import com.stock.survive.dto.TradeHistoryDto;
import com.stock.survive.entity.User;


public interface TradeHistoryService {

//    void processSell(User user, String ticker, int volume, double price);

    TradeHistoryDto processBuy(Long userNo, String ticker, Long price, Integer volume);
}
