package com.stock.survive.repository;

import com.stock.survive.entity.AccountHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountHistoryRepository extends JpaRepository<AccountHistory, Long> {
    
    List<AccountHistory> findByUserNoOrderByCreatedAtDesc(Integer userNo);
    
    @Query("SELECT a FROM AccountHistory a WHERE a.userNo = :userNo ORDER BY a.createdAt DESC")
    List<AccountHistory> findLatestByUser(@Param("userNo") Integer userNo);
    
    @Query(value = "SELECT ah.remain FROM account_histories ah WHERE ah.user_no = :userNo ORDER BY ah.created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Long> findLatestRemainByUserNo(@Param("userNo") Integer userNo);
}
