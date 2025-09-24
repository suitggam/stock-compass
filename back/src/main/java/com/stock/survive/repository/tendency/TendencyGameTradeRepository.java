package com.stock.survive.repository.tendency;

import com.stock.survive.entity.tendency.TendencyGameSession;
import com.stock.survive.entity.tendency.TendencyGameTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TendencyGameTradeRepository extends JpaRepository<TendencyGameTrade, Long> {

    List<TendencyGameTrade> findBySessionAndWeekIndexOrderByExecutedAtAsc(TendencyGameSession session, Integer weekIndex);

    List<TendencyGameTrade> findBySessionOrderByExecutedAtDesc(TendencyGameSession session);
}
