package dev.a301.stock.dto.user.response;

import java.time.LocalDateTime;

public record UserProfileResponse(
  Integer userNo,
  String nickname,
  String socialEmail,
  LocalDateTime createdAt,
  Integer totalReward,
  Integer cash
) {}