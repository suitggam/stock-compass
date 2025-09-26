package com.stock.survive.repository;

import com.stock.survive.entity.GameResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameResultRepository extends JpaRepository<GameResultEntity, Long> {
    
    Optional<GameResultEntity> findTopByUserNoOrderByCreatedAtDesc(Long userNo);
}