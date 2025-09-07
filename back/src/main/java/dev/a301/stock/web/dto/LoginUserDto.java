package dev.a301.stock.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoginUserDto(
        Integer userNo,
        String  socialEmail,
        String  nickname,
        Boolean cancel,
        LocalDateTime createdAt,
        Integer top1,
        Integer top2,
        Integer top3,
        Integer topten,
        BigDecimal asset,
        BigDecimal cash
) {}
