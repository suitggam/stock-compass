package com.stock.survive.service;

import java.time.LocalDate;
import java.time.LocalTime;

public interface TradingHoursService {
    
    boolean isMarketOpen();
    
    boolean isTradingDay();
    
    boolean isTradingTime();
    
    LocalTime getMarketOpenTime();
    
    LocalTime getMarketCloseTime();
    
    boolean isWeekend();
    
    boolean isHoliday(LocalDate date);
}
