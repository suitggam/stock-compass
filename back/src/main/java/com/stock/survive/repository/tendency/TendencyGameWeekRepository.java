package com.stock.survive.repository.tendency;

import com.stock.survive.entity.tendency.TendencyGameSession;
import com.stock.survive.entity.tendency.TendencyGameWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TendencyGameWeekRepository extends JpaRepository<TendencyGameWeek, Long> {

    List<TendencyGameWeek> findBySessionOrderByWeekIndexAsc(TendencyGameSession session);
}
