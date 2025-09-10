package dev.a301.stock.service.auth;

import dev.a301.stock.entity.auth.RefreshToken;
import dev.a301.stock.entity.user.User;
import dev.a301.stock.repository.auth.RefreshTokenRepository;
import dev.a301.stock.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ✅ 추가

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
  private final RefreshTokenRepository repo;
  private final UserRepository userRepo;

  @Transactional
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

  @Transactional(readOnly = true)
  public Optional<RefreshToken> findValid(String tokenHash) {
    // ✅ fetch join 버전으로 교체하여 rt.getUser() 접근 시 LAZY 예외 방지
    return repo.findValidWithUser(tokenHash, LocalDateTime.now());
  }

  @Transactional
  public void revoke(RefreshToken rt) {
    rt.setRevoked(true);
    repo.save(rt);
  }
}
