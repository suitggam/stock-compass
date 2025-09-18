package com.stock.survive.support;

import java.time.LocalDate;

public interface StockCandlePointView {
    LocalDate getDate();
    Integer getOpen();
    Integer getHigh();
    Integer getLow();
    Integer getClose();
    Long getVolume();
}

