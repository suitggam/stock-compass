package dev.a301.stock.service.auth;

import dev.a301.stock.entity.auth.RefreshToken;
import dev.a301.stock.entity.user.User;
import dev.a301.stock.global.util.HashUtils;
import dev.a301.stock.repository.auth.RefreshTokenRepository;
import dev.a301.stock.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

  private final RefreshTokenRepository repo;
  private final UserRepository userRepo;

  /** 로그인 시 최초 저장 (tokenHash는 sha256Hex 원문 해시여야 함) */
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
    RefreshToken saved = repo.save(rt);
    log.info("[RT-SAVE] uid={} hash.prefix={} exp={} ua={} ip={}",
        userNo, prefix(tokenHash), expiresAt, safe(ua), safe(ip));
    return saved;
  }

  /** 평상시 조회(잠금 없음) */
  public Optional<RefreshToken> findValid(String tokenHash) {
    Optional<RefreshToken> r = repo.findByTokenHashAndRevokedFalseAndExpiresAtAfter(tokenHash, LocalDateTime.now());
    log.debug("[RT-FIND] hash.prefix={} -> found={}", prefix(tokenHash), r.isPresent());
    return r;
  }

  /** 회전용: 유효 토큰을 잠금으로 집어오고, 곧바로 하드 삭제 */
  @Transactional
  public User consumeAndGetUser(String rawRefresh) {
    String hash = HashUtils.sha256Hex(rawRefresh);
    LocalDateTime now = LocalDateTime.now();
    log.info("[RT-CONSUME] raw.prefix={} hash.prefix={} at={}", prefix(rawRefresh), prefix(hash), now);

    RefreshToken rt = repo.findValidForUpdate(hash, now)
        .orElseThrow(() -> {
          log.warn("[RT-CONSUME] not-found-or-expired hash.prefix={} at={}", prefix(hash), now);
          return new IllegalStateException("invalid/expired refresh"); // 컨트롤러에서 401 매핑
        });

    if (rt.getExpiresAt().isBefore(now)) {
      log.warn("[RT-CONSUME] expired uid={} exp={} now={}", rt.getUser().getUserNo(), rt.getExpiresAt(), now);
      throw new IllegalStateException("expired refresh");
    }

    User user = rt.getUser();
    repo.deleteById(rt.getId()); // 하드 삭제(회전)
    log.info("[RT-CONSUME] deleted id={} uid={} ok", rt.getId(), user.getUserNo());
    return user;
  }

  /** 로그아웃 시: 있으면 하드 삭제 */
  @Transactional
  public void deleteOnLogout(String rawRefresh) {
    if (rawRefresh == null || rawRefresh.isBlank()) {
      log.debug("[RT-LOGOUT] no cookie");
      return;
    }
    String hash = HashUtils.sha256Hex(rawRefresh);
    repo.findValidForUpdate(hash, LocalDateTime.now())
        .ifPresent(r -> {
          repo.deleteById(r.getId());
          log.info("[RT-LOGOUT] deleted id={} uid={}", r.getId(), r.getUser().getUserNo());
        });
  }

  /** 배치/관리자용: 만료분 정리(선택) */
  @Transactional
  public int purgeExpired() {
    int n = repo.deleteAllExpired(LocalDateTime.now());
    log.info("[RT-PURGE] deletedExpired={}", n);
    return n;
  }

  private static String prefix(String s) {
    if (s == null) return "null";
    return s.length() <= 10 ? s : s.substring(0, 10);
  }

  private static String safe(String s) {
    return s == null ? "" : (s.length() > 200 ? s.substring(0, 200) + "..." : s);
  }
}
