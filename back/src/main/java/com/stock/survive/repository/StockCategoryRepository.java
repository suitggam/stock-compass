package com.stock.survive.repository;

import com.stock.survive.entity.StockCategory;
import com.stock.survive.entity.StockItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockCategoryRepository extends JpaRepository<StockCategory,Long> {

}
