package com.stock.survive.repository;


import com.stock.survive.entity.StockInfos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockInfosRepository extends JpaRepository<StockInfos, Long> {



}
