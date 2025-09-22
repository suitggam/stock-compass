package com.stock.survive.dto;

import com.stock.survive.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPageDto {
    private Integer userNo;
    private String socialEmail;
    private String nickname;
    private boolean cancel;
    private Integer totalReward;
    private Integer cash;
    private String createdAt;

    private String avatarUrl;

    public static UserSummaryDto of(User u) {
        return new UserSummaryDto(
                u.getId(), u.getSocialEmail(), u.getNickname(), u.isCancel(),
                u.getTotalReward(), u.getCash(),
                u.getCreatedAt() != null ? u.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                null // avatarUrl 기본값
        );
    }
    //관심종목

    //게임 결과

    //모의투자 기록


}
