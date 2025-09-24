package com.stock.survive.repository;

import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.entity.StockInfos;
import com.stock.survive.entity.StockItems;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockItemsRepository extends JpaRepository<StockItems, Long> {
    
    @Query(
            value = "SELECT new com.stock.survive.dto.StockEndDayDto(" +
                    "si.ticker, si.companyName, inf.startPrice, inf.endPrice, inf.volume, si.category.categoryName, inf.marketCap) " +
                    "FROM StockItems si JOIN si.infos inf " +
                    "WHERE FUNCTION('DATE', inf.date) = :targetDate " +
                    "ORDER BY si.itemNo ASC",
            countQuery = "SELECT count(si) FROM StockItems si JOIN si.infos inf WHERE FUNCTION('DATE', inf.date) = :targetDate"
    )
    Page<StockEndDayDto> getEndOfDayData(@Param("targetDate") LocalDate targetDate, Pageable pageable);
    
    @Query("SELECT MAX(inf.date) FROM StockInfos inf")
    LocalDate findMaxDate();
    
    Optional<StockItems> findCompanyNameByTicker(String ticker);
}