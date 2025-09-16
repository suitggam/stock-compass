package com.stock.survive.dto;

import com.stock.survive.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private Integer userNo;
    private String socialEmail;
    private String nickname;
    private boolean cancel;
    private Integer totalReward;
    private Integer cash;
    private String createdAt;

    public static UserSummaryDto of(User u) {
        return new UserSummaryDto(
                u.getId(),
                u.getSocialEmail(),
                u.getNickname(),
                u.isCancel(),
                u.getTotalReward(),
                u.getCash(),
                u.getCreatedAt() != null ? u.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
        );
    }
}

