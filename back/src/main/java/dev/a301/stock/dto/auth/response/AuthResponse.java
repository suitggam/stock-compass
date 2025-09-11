package dev.a301.stock.dto.auth.response;

import dev.a301.stock.dto.user.response.UserSummaryResponse;

public record AuthResponse(
  UserSummaryResponse user,
  String accessToken
) {}