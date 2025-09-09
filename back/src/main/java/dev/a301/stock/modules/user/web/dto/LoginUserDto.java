package dev.a301.stock.modules.user.web.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class LoginUserDto {
    public Integer userNo;
    public String  socialEmail;
    public String  nickname;
    public Boolean cancel;
    public LocalDateTime createdAt;
    public Integer top1;
    public Integer top2;
    public Integer top3;
    public Integer topten;
    public BigDecimal asset;
    public BigDecimal cash;

    public LoginUserDto(
        Integer userNo, String socialEmail, String nickname, Boolean cancel, LocalDateTime createdAt,
        Integer top1, Integer top2, Integer top3, Integer topten, BigDecimal asset, BigDecimal cash
    ) {
        this.userNo = userNo; this.socialEmail = socialEmail; this.nickname = nickname; this.cancel = cancel;
        this.createdAt = createdAt; this.top1 = top1; this.top2 = top2; this.top3 = top3; this.topten = topten;
        this.asset = asset; this.cash = cash;
    }
}