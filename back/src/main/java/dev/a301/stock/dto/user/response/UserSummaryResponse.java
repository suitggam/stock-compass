package dev.a301.stock.dto.user.response;

public record UserSummaryResponse(
  Integer userNo,
  String nickname,
  String socialEmail
) {}