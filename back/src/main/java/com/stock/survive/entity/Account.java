package com.stock.survive.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_no")
    private Long accountNo;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_no", foreignKey = @ForeignKey(name = "fk_accounts_user"))
    private User user;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TradeHistory> tradeHistories = new ArrayList<>();

    @Builder.Default
    @Column(name = "total_reward", nullable = false)
    private Integer totalReward = 0;

    @Builder.Default
    @Column(name = "original_money",nullable = false)
    private Long originalMoney = 10_000_000L;

    @Builder.Default
    @Column(name = "cash",nullable = false)
    private Long cash = 10_000_000L;

    @Builder.Default
    @Column(name = "haveStock",nullable = false)
    private Long haveStock=0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Account createDefault(User user) {
        Account acc = new Account();
        acc.user = user;
        acc.accountNo = null;
        acc.totalReward = 0;
        acc.originalMoney = 10_000_000L;
        acc.cash = 10_000_000L;
        acc.haveStock = 0L;
        acc.tradeHistories = new ArrayList<>();
        return acc;
    }

    // 시간 처리 함수
    @PrePersist
    void onCreate() {
        // createdAt/updatedAt 자동 세팅
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }


}
