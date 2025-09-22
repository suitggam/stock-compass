package com.stock.survive.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "game_results")
public class GameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_no")
    private Long gameNo;

    @Column(name = "user_no", nullable = false)
    private Integer userNo;

    @Column(name = "game_time", nullable = false)
    private LocalDateTime gameTime;

    @Column(name = "response_count", nullable = false)
    private Integer responseCount;

    @Column(name = "short_sell_count", nullable = false)
    private Integer shortSellCount;

    @Column(name = "sell_count", nullable = false)
    private Integer sellCount;

    @Column(name = "buy_count", nullable = false)
    private Integer buyCount;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "final_asset")
    private Long finalAsset;
}

