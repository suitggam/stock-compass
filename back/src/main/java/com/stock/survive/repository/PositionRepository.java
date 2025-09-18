package com.stock.survive.repository;

import com.stock.survive.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    Optional<Position> findByUserNoAndStockNo(Integer userNo, Integer stockNo);
    
    List<Position> findByUserNo(Integer userNo);
    
    @Query("SELECT p FROM Position p WHERE p.userNo = :userNo AND p.stockCnt > 0")
    List<Position> findActivePositionsByUser(@Param("userNo") Integer userNo);
}
