package com.stock.survive.repository;

import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.dto.StockItemOptionDto;
import com.stock.survive.support.StockPricePointView;
import com.stock.survive.support.StockCandlePointView;
import com.stock.survive.entity.StockItems;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockItemsRepository extends JpaRepository<StockItems, Integer> {

    @Query(
            value = "SELECT new com.stock.survive.dto.StockEndDayDto(si.ticker, si.companyName, inf.startPrice, inf.endPrice, inf.volume, si.category.categoryName, inf.marketCap) " +
                    "FROM StockItems si JOIN si.infos inf WHERE inf.date = :targetDate  ORDER BY si.itemNo ASC",
            countQuery = "SELECT count(si) FROM StockItems si JOIN si.infos inf WHERE inf.date = :targetDate"
    )
    Page<StockEndDayDto> getEndOfDayData(@Param("targetDate") LocalDate targetDate, Pageable pageable);

    @Query("SELECT new com.stock.survive.dto.StockItemOptionDto(si.itemNo, si.ticker, si.companyName) " +
            "FROM StockItems si ORDER BY si.itemNo ASC")
    List<StockItemOptionDto> findAllOptions();

    @Query(value = """
            SELECT sd.date as date, sd.close_price as endPrice
            FROM stock_data sd
            JOIN stock_items si ON si.ticker = sd.ticker
            WHERE si.item_no = :itemNo
              AND sd.date BETWEEN :from AND :to
            ORDER BY sd.date ASC
            """, nativeQuery = true)
    List<StockPricePointView> findHistoryFromStockData(@Param("itemNo") Integer itemNo,
                                                       @Param("from") java.time.LocalDate from,
                                                       @Param("to") java.time.LocalDate to);

    @Query(value = """
            SELECT sd.date as date,
                   sd.open_price as open,
                   sd.high_price as high,
                   sd.low_price  as low,
                   sd.close_price as close,
                   sd.volume as volume
            FROM stock_data sd
            JOIN stock_items si ON si.ticker = sd.ticker
            WHERE si.item_no = :itemNo
              AND sd.date BETWEEN :from AND :to
            ORDER BY sd.date ASC
            """, nativeQuery = true)
    List<StockCandlePointView> findCandleFromStockData(@Param("itemNo") Integer itemNo,
                                                       @Param("from") java.time.LocalDate from,
                                                       @Param("to") java.time.LocalDate to);

}
