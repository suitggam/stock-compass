package dev.a301.stock.service.auth;

import dev.a301.stock.entity.auth.RefreshToken;
import dev.a301.stock.entity.user.User;
import dev.a301.stock.global.util.HashUtils;
import dev.a301.stock.repository.auth.RefreshTokenRepository;
import dev.a301.stock.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository repo;
  private final UserRepository userRepo;

  /** 로그인 시 최초 저장 */
  public RefreshToken save(Integer userNo, String tokenHash, LocalDateTime expiresAt, String ua, String ip) {
    User user = userRepo.findById(userNo).orElseThrow();
    RefreshToken rt = RefreshToken.builder()
        .user(user)
        .tokenHash(tokenHash)
        .issuedAt(LocalDateTime.now())
        .expiresAt(expiresAt)
        .userAgent(ua)
        .ip(ip)
        .revoked(false)
        .build();
    return repo.save(rt);
  }

  /** 평상시 조회(잠금 없음) */
  public Optional<RefreshToken> findValid(String tokenHash) {
    return repo.findByTokenHashAndRevokedFalseAndExpiresAtAfter(tokenHash, LocalDateTime.now());
  }

  /** 회전용: 유효 토큰을 잠금으로 집어오고, 곧바로 하드 삭제 */
  @Transactional
  public User consumeAndGetUser(String rawRefresh) {
    String hash = HashUtils.sha256Hex(rawRefresh);
    RefreshToken rt = repo.findValidForUpdate(hash, LocalDateTime.now())
        .orElseThrow(); // 컨트롤러에서 401 처리
    User user = rt.getUser();
    repo.deleteById(rt.getId());      // ★ 하드 삭제
    return user;
  }

  /** 로그아웃 시: 있으면 하드 삭제 */
  @Transactional
  public void deleteOnLogout(String rawRefresh) {
    if (rawRefresh == null || rawRefresh.isBlank()) return;
    String hash = HashUtils.sha256Hex(rawRefresh);
    repo.findValidForUpdate(hash, LocalDateTime.now())
        .ifPresent(r -> repo.deleteById(r.getId()));
  }

  /** 배치/관리자용: 만료분 정리(선택) */
  @Transactional
  public int purgeExpired() {
    return repo.deleteAllExpired(LocalDateTime.now());
  }
}
