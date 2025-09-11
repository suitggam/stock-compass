package dev.a301.stock.repository.auth;

import dev.a301.stock.entity.auth.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /** 회수(소비) 시 동시성 제어를 위한 행 잠금 조회 + user 즉시 로딩 */
  @EntityGraph(attributePaths = "user")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select r
      from RefreshToken r
      where r.tokenHash = :hash
        and r.revoked = false
        and r.expiresAt > :now
      """)
  Optional<RefreshToken> findValidForUpdate(@Param("hash") String hash,
                                            @Param("now") LocalDateTime now);

  /** 평상시 유효성 확인용(잠금 없음) + user 즉시 로딩 */
  @EntityGraph(attributePaths = "user")
  Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(
      String tokenHash, LocalDateTime now
  );

  /** 만료 토큰 일괄 정리(선택) */
  @Modifying
  @Query("delete from RefreshToken r where r.expiresAt <= :now")
  int deleteAllExpired(@Param("now") LocalDateTime now);
}
