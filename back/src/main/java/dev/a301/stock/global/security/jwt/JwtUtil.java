package dev.a301.stock.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
  private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

  private final SecretKey key;
  private final long accessExp;   // seconds
  private final long refreshExp;  // seconds

  public JwtUtil(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-exp-seconds}") long accessExp,
      @Value("${jwt.refresh-exp-seconds}") long refreshExp
  ) {
    // 1) 공백/따옴표 제거
    String s = (secret == null ? "" : secret.trim());
    if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
      s = s.substring(1, s.length() - 1);
    }

    // 2) Base64 우선 사용, 실패 시 UTF-8 바이트 폴백
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(s);
    } catch (IllegalArgumentException e) {
      keyBytes = s.getBytes(StandardCharsets.UTF_8);
    }

    // 3) 길이 검증(>=32 bytes = 256 bits)
    if (keyBytes.length < 32) {
      throw new IllegalArgumentException(
          "JWT secret must be >= 256 bits (32 bytes). current=" + (keyBytes.length * 8) + " bits");
    }

    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessExp = accessExp;
    this.refreshExp = refreshExp;

    log.info("JWT key length = {} bits", keyBytes.length * 8);
  }

  /* ===================== 발급 ===================== */

  // Access 토큰: 최소 필요한 클레임만 (uid, typ=access)
  public String issueAccess(Integer userNo, String nickname) {
    var now = Instant.now();
    return Jwts.builder()
        .setSubject(String.valueOf(userNo))
        .addClaims(Map.of(
            "uid", userNo,
            "nickname", nickname,
            "typ", "access"
        ))
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plusSeconds(accessExp)))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  // Refresh 토큰: PII 최소화 권장(typ=refresh 만)
  public String issueRefresh(Integer userNo) {
    var now = Instant.now();
    return Jwts.builder()
        .setSubject(String.valueOf(userNo))
        .addClaims(Map.of("typ", "refresh"))
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plusSeconds(refreshExp)))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  /* ====== (선택) 기존 generate* 유지하고 싶으면 아래처럼 래핑 ======
  public String generateAccessToken(Integer userNo, String socialEmail, String nickname) {
    // 필요하면 email도 넣되, 진짜 필요한지 검토
    return issueAccess(userNo, nickname);
  }

  public String generateRefreshToken(Integer userNo, String socialEmail) {
    // refresh에 email을 넣지 않는 걸 권장
    return issueRefresh(userNo);
  }
  */

  /* ===================== 파싱/검증 헬퍼 ===================== */

  public boolean validate(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public String getSubject(String token) {
    var claims = Jwts.parserBuilder().setSigningKey(key).build()
        .parseClaimsJws(token).getBody();
    return claims.getSubject();
  }

  public Instant getExpiry(String token) {
    var claims = Jwts.parserBuilder().setSigningKey(key).build()
        .parseClaimsJws(token).getBody();
    return claims.getExpiration().toInstant();
  }

  public boolean isAccess(String token) {
    try {
      var claims = Jwts.parserBuilder().setSigningKey(key).build()
          .parseClaimsJws(token).getBody();
      Object typ = claims.get("typ");
      return "access".equals(typ);
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public boolean isRefresh(String token) {
    try {
      var claims = Jwts.parserBuilder().setSigningKey(key).build()
          .parseClaimsJws(token).getBody();
      Object typ = claims.get("typ");
      return "refresh".equals(typ);
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  /* ===================== 필요시 원시 파서 ===================== */
  public Jws<Claims> parse(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
  }
}
