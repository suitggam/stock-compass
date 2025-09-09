package dev.a301.stock.common.security.jwt;

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
  private final long accessExp;
  private final long refreshExp;

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

    // 2) Base64 우선 사용 (권장). 실패하면 UTF-8 바이트로 폴백.
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(s);
    } catch (IllegalArgumentException e) {
      // Base64가 아니면 그대로 UTF-8 바이트 사용
      keyBytes = s.getBytes(StandardCharsets.UTF_8);
    }

    // 3) 길이 검증(>= 32 bytes = 256 bits)
    if (keyBytes.length < 32) {
      throw new IllegalArgumentException(
          "JWT secret must be >= 256 bits (32 bytes). current=" + (keyBytes.length * 8) + " bits");
    }

    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessExp = accessExp;
    this.refreshExp = refreshExp;

    log.info("JWT key length = {} bits", keyBytes.length * 8);
  }

  public String issueAccess(Integer userNo) {
    var now = Instant.now();
    return Jwts.builder()
        .setSubject(String.valueOf(userNo))
        .addClaims(Map.of("typ", "access"))
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plusSeconds(accessExp)))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

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

  public Jws<Claims> parse(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
  }
}
