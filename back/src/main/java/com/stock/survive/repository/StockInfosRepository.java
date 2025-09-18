package com.stock.survive.repository;


import com.stock.survive.dto.StockInfosDto;
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

    @Query("SELECT s FROM StockInfos s " +
            "JOIN FETCH s.stockItem si " +
            "WHERE si.ticker = :ticker " +
            "ORDER BY s.date ASC")
    List<StockInfos> findRecent6YearsByTicker(@Param("ticker") String ticker);
}
