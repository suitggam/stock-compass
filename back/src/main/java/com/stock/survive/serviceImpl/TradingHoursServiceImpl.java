package com.stock.survive.serviceImp;

import com.stock.survive.service.TradingHoursService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class TradingHoursServiceImpl implements TradingHoursService {

    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);
    
    // 2025년 공휴일 목록
    private static final List<LocalDate> HOLIDAYS_2025 = Arrays.asList(
            LocalDate.of(2025, 1, 1),   // 신정
            LocalDate.of(2025, 1, 28),  // 설날
            LocalDate.of(2025, 1, 29),  // 설날
            LocalDate.of(2025, 1, 30),  // 설날
            LocalDate.of(2025, 3, 1),   // 삼일절
            LocalDate.of(2025, 5, 5),   // 어린이날
            LocalDate.of(2025, 6, 6),   // 현충일
            LocalDate.of(2025, 8, 15),  // 광복절
            LocalDate.of(2025, 10, 3),  // 개천절
            LocalDate.of(2025, 10, 9),  // 한글날
            LocalDate.of(2025, 12, 25)  // 성탄절
    );

    @Override
    public boolean isMarketOpen() {
        return isTradingDay() && isTradingTime();
    }

    @Override
    public boolean isTradingDay() {
        LocalDate today = LocalDate.now();
        return !isWeekend() && !isHoliday(today);
    }

    @Override
    public boolean isTradingTime() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(MARKET_OPEN) && !now.isAfter(MARKET_CLOSE);
    }

    @Override
    public LocalTime getMarketOpenTime() {
        return MARKET_OPEN;
    }

    @Override
    public LocalTime getMarketCloseTime() {
        return MARKET_CLOSE;
    }

    @Override
    public boolean isWeekend() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        return HOLIDAYS_2025.contains(date);
    }
}
