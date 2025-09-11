package dev.a301.stock.service.auth;

import dev.a301.stock.entity.auth.RefreshToken;
import dev.a301.stock.entity.user.User;
import dev.a301.stock.global.util.HashUtils;
import dev.a301.stock.repository.auth.RefreshTokenRepository;
import dev.a301.stock.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository repo;
  private final UserRepository userRepo;

  public RefreshToken save(Integer userNo, String tokenHash,
                           LocalDateTime expiresAt, String ua, String ip) {
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
    log.info("[RT-SAVE] uid={} hash.prefix={} exp={}", userNo, prefix(tokenHash), expiresAt);
    return saved;
  }

  @Transactional
  public Optional<Integer> consumeIfValid(String rawRefreshJwt) {
    if (rawRefreshJwt == null || rawRefreshJwt.isBlank()) return Optional.empty();
    String hash = HashUtils.sha256Hex(rawRefreshJwt);
    return consumeByHash(hash);
  }

  @Transactional
  public Optional<Integer> consumeByHash(String tokenHash) {
    return repo.findValidForUpdate(tokenHash, LocalDateTime.now()).map(rt -> {
      Integer userNo = rt.getUser().getUserNo();
      repo.delete(rt); // 1회용 보장
      log.info("[RT-CONSUME] deleted id={} uid={} hash.prefix={}",
          rt.getId(), userNo, prefix(tokenHash));
      return userNo;
    });
  }

  @Transactional
  public int purgeExpired() {
    return repo.deleteAllExpired(LocalDateTime.now());
  }

  private static String prefix(String s) {
    if (s == null) return "null";
    return s.length() <= 10 ? s : s.substring(0, 10);
  }
}
