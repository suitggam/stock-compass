package dev.a301.stock.service.auth;

import dev.a301.stock.global.security.jwt.JwtUtil;
import dev.a301.stock.global.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

  /**
   * refresh 토큰 발급 + DB 저장(해시)
   * - 컨트롤러에서는 이 메서드만 호출하고, 별도의 save()를 다시 부르지 마세요(중복 저장 방지).
   */
  public String issueRefreshToken(Integer userNo, String userAgent, String ip) {
    String rawRefresh = jwtUtil.issueRefresh(userNo);

    // 만료시각 계산
    LocalDateTime expLdt = getRefreshExpiry(rawRefresh);

    // 해시 저장
    String hash = HashUtils.sha256Hex(rawRefresh);
    refreshTokenService.save(userNo, hash, expLdt, userAgent, ip);

    // HttpOnly 쿠키로 내려보낼 원문 반환
    return rawRefresh;
  }

  /** refresh JWT의 만료 시각을 LocalDateTime으로 반환 */
  public LocalDateTime getRefreshExpiry(String refreshJwt) {
    // JwtUtil.getExpiry(...) 가 Instant (또는 Date->Instant) 를 반환한다고 가정
    return jwtUtil.getExpiry(refreshJwt)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();
  }

  public boolean validateAccess(String token) {
    return jwtUtil.validate(token);
  }
}
