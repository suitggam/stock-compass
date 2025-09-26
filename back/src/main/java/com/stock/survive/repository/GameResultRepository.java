package com.stock.survive.repository;

import com.stock.survive.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameResultRepository extends JpaRepository<GameResult, Long> {
    
    Optional<GameResult> findTopByUserNoOrderByCreatedAtDesc(Long userNo);
}