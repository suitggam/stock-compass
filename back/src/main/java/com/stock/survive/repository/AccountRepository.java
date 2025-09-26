package com.stock.survive.repository;

import com.stock.survive.entity.Account;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // 새 구현 -> 유저에서 계좌로 옮긴것
    @Transactional
    @Modifying
    @Query("UPDATE Account a SET a.cash = :cash, a.haveStock = :haveStock WHERE a.accountNo = :accountNo")
    void updateCashAndHaveStock(@Param("accountNo") Long accountNo, @Param("cash") Long cash, @Param("haveStock") Long haveStock);
}
