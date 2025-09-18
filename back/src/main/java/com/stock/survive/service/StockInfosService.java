package com.stock.survive.service;

import com.stock.survive.dto.StockInfosDto;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface StockInfosService {

    List<StockInfosDto> getStock(String ticker);

}
