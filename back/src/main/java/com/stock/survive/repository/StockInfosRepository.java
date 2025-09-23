package com.stock.survive.repository;


import com.stock.survive.entity.StockInfos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockInfosRepository extends JpaRepository<StockInfos, Long> {


    @Query(value = "SELECT inf.end_price FROM stock_infos inf WHERE inf.item_no = :itemNo ORDER BY inf.date DESC LIMIT 1", nativeQuery = true)
    Optional<Integer> findLatestEndPriceByItemNo(@Param("itemNo") Integer itemNo);

    @Query("SELECT s FROM StockInfos s " +
            "JOIN FETCH s.stockItem si " +
            "WHERE si.ticker = :ticker " +
            "ORDER BY s.date DESC")
    List<StockInfos> findRecent6YearsByTicker(@Param("ticker") String ticker);




}
