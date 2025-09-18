package com.stock.survive.support;

import java.time.LocalDate;

public interface StockPricePointView {
    LocalDate getDate();
    Integer getEndPrice();
}

