package com.stock.survive.dto.tendency;

import java.time.Duration;
import java.time.LocalDateTime;

public record TendencyGameResultResponse(
        Long sessionId,
        int maxWeek,
        int finalWeek,
        int totalAsset,
        long realizedProfit,
        double totalYield,
        boolean yieldAboveThreshold,
        String tendencyType,
        String recommendation,
        long decisionElapsedSeconds,
        int volatileBuyCount,
        int volatileSellCount,
        int sellDominantWeekCount,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
