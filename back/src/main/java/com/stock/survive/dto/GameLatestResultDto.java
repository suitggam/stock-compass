package com.stock.survive.dto;

import com.stock.survive.entity.GameResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameLatestResultDto {
    private LocalDateTime gameTime;
    private Integer responseCount;
    private Integer shortSellCount;
    private Integer sellCount;
    private Integer buyCount;
    private Double rate;
    private Long finalAsset;

    public static GameLatestResultDto from(GameResult gr) {
        if (gr == null) return null;
        return GameLatestResultDto.builder()
                .gameTime(gr.getGameTime())
                .responseCount(gr.getResponseCount())
                .shortSellCount(gr.getShortSellCount())
                .sellCount(gr.getSellCount())
                .buyCount(gr.getBuyCount())
                .rate(gr.getRate())
                .finalAsset(gr.getFinalAsset())
                .build();
    }
}

