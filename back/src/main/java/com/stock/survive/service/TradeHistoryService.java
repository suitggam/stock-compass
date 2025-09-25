package com.stock.survive.service;

import com.stock.survive.dto.TradeHistoryDto;
import com.stock.survive.dto.UserAssetDto;


public interface TradeHistoryService {

    UserAssetDto getUserAssets(Long userNo);
    TradeHistoryDto processBuy(Long userNo, String ticker, Long price, Integer volume);
    TradeHistoryDto processSell(Long userNo, String ticker, Long price, Integer volume);
}
