package dev.a301.stock.repository.auth;

import dev.a301.stock.entity.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  // 기존 단순 조회 (필요하면 남겨둬도 됨)
  Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(String tokenHash, LocalDateTime now);

  // ✅ LAZY 문제 해결용: user를 함께 로딩
  @Query("""
    select rt
    from RefreshToken rt
    join fetch rt.user u
    where rt.tokenHash = :tokenHash
      and rt.revoked = false
      and rt.expiresAt > :now
  """)
  Optional<RefreshToken> findValidWithUser(@Param("tokenHash") String tokenHash,
                                           @Param("now") LocalDateTime now);
}
