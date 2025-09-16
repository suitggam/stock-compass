package com.stock.survive.repository;

import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.dto.StockRealtimeDto;
import com.stock.survive.entity.StockRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockRealtimeRepository extends JpaRepository<StockRealtime, Long> {


    @Query("SELECT new com.stock.survive.dto.StockRealtimeDto(" +
            "si.ticker, inf.volume, inf.marketCap, sc.categoryName) " +
            "FROM StockItems si " +
            "JOIN si.category sc " +
            "JOIN si.infos inf " +
            "WHERE inf.date = (" +
            "   SELECT MAX(sinf.date) FROM StockInfos sinf WHERE sinf.stockItem = si" +
            ")")
    List<StockRealtimeDto> getAllStockRealtimeWithLatestInfo();

//    @Query("SELECT new com.stock.survive.dto.StockEndDayDto(" +
//            "si.ticker, si.companyName, inf.startPrice, inf.endPrice, " +
//            "inf.volume, si.category.categoryName, inf.marketCap) " +
//            "FROM StockItems si " +
//            "JOIN si.infos inf " +
//            "WHERE inf.date = :targetDate")
//    List<StockEndDayDto> getEndOfDayData(@Param("targetDate") LocalDate targetDate);
}
