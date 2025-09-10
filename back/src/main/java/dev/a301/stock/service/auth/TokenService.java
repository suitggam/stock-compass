package dev.a301.stock.service.auth;

import dev.a301.stock.global.security.jwt.JwtUtil;
import dev.a301.stock.global.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TokenService {

  private final JwtUtil jwtUtil;
  private final RefreshTokenService refreshTokenService;

  /** access 토큰 발급 (닉네임을 클레임에 포함하는 정책) */
  public String issueAccessToken(Integer userNo, String nickname) {
    return jwtUtil.issueAccess(userNo, nickname);
  }

  /** refresh 토큰 발급 + DB 저장(해시) */
  public String issueRefreshToken(Integer userNo, String userAgent, String ip) {
    String rawRefresh = jwtUtil.issueRefresh(userNo);
    String hash = HashUtils.sha256Hex(rawRefresh);

    var expLdt = jwtUtil.getExpiry(rawRefresh)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();

    refreshTokenService.save(userNo, hash, expLdt, userAgent, ip);
    return rawRefresh; // HttpOnly 쿠키로 내려보낼 원문
  }

  public boolean validateAccess(String token) {
    return jwtUtil.validate(token);
  }
}
