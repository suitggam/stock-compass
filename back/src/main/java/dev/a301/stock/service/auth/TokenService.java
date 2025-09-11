package dev.a301.stock.service.auth;

import dev.a301.stock.entity.user.User;
import dev.a301.stock.global.security.jwt.JwtUtil;
import dev.a301.stock.global.util.HashUtils;
import dev.a301.stock.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TokenService {

  private final JwtUtil jwtUtil;
  private final RefreshTokenService refreshTokenService;
  private final UserRepository userRepo;

  /* ===== Access ===== */
  public String issueAccessToken(Integer userNo, String nickname) {
    return jwtUtil.issueAccess(userNo, nickname);
  }

  public String issueAccessTokenByUserId(Integer userNo) {
    User u = userRepo.findById(userNo).orElseThrow();
    return jwtUtil.issueAccess(userNo, u.getNickname());
  }

  public boolean validateAccess(String token) {
    return jwtUtil.validate(token);
  }

  /* ===== Refresh (원문 반환 + DB 저장) ===== */
  public String issueRefreshToken(Integer userNo, String userAgent, String ip) {
    String rawRefresh = jwtUtil.issueRefresh(userNo);                              // 1) 발급
    LocalDateTime expLdt = jwtUtil.getExpiry(rawRefresh).atZone(ZoneId.systemDefault()).toLocalDateTime();
    String hash = HashUtils.sha256Hex(rawRefresh);
    refreshTokenService.save(userNo, hash, expLdt, userAgent, ip);                // 2) 저장(1회용)
    return rawRefresh;                                                             // 3) 쿠키로 내려보낼 원문
  }
}
