package com.stock.survive.repository.tendency;

import com.stock.survive.entity.StockInfos;
import com.stock.survive.entity.tendency.TendencyGameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TendencyGameSessionRepository extends JpaRepository<TendencyGameSession, Long> {

    Optional<TendencyGameSession> findByIdAndUser_Id(Long id, Integer userId);

    List<TendencyGameSession> findByUser_IdOrderByStartedAtDesc(Integer userId);
    
    List<TendencyGameSession> findByTickerAndStartedAtBetween(String ticker, LocalDateTime startDate, LocalDateTime endDate);
}
