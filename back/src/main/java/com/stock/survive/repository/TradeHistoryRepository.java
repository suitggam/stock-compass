package com.stock.survive.repository;

import com.stock.survive.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {
    
    List<TradeHistory> findByUserNoAndStatus(Integer userNo, TradeHistory.OrderStatus status);
    
    List<TradeHistory> findByStockNoAndStatus(Integer stockNo, TradeHistory.OrderStatus status);
    
    List<TradeHistory> findByStatus(TradeHistory.OrderStatus status);
    
    @Query("SELECT t FROM TradeHistory t WHERE t.status = 'PENDING' AND t.expiresAt < :now")
    List<TradeHistory> findExpiredOrders(@Param("now") LocalDateTime now);
    
    @Query("SELECT t FROM TradeHistory t WHERE t.status = 'PENDING' AND t.orderType = 'LIMIT' AND t.stockNo = :stockNo")
    List<TradeHistory> findPendingLimitOrdersByStock(@Param("stockNo") Integer stockNo);
    
    @Query("SELECT t FROM TradeHistory t WHERE t.status = 'PENDING' AND t.orderType IN ('STOP_LOSS', 'TAKE_PROFIT') AND t.stockNo = :stockNo")
    List<TradeHistory> findPendingConditionalOrdersByStock(@Param("stockNo") Integer stockNo);
}
