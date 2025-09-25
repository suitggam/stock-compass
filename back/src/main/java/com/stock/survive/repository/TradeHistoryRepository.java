package com.stock.survive.repository;

import com.stock.survive.entity.StockInfos;
import com.stock.survive.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {
    List<TradeHistory> findByUser_Id(Long userNo);

    // 특정 사용자의 특정 종목에 대한 최신 거래 내역을 조회
    Optional<TradeHistory> findTopByUser_IdAndStockItems_ItemNoOrderByCreateAtDesc(Long userNo, Long itemNo);

    @Query("SELECT s FROM StockInfos s " +
            "JOIN FETCH s.stockItem si " +
            "WHERE si.ticker = :ticker " +
            "ORDER BY s.date DESC")
    List<StockInfos> findRecent6YearsByTicker(@Param("ticker") String ticker);

}
