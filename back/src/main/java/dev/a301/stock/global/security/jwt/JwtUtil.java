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
import java.security.MessageDigest;
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
    String normalized = stripQuotes(secret);

    // 1) 명시 프리픽스 우선: base64:, base64url:, plain:
    byte[] keyBytes = null;
    if (normalized.startsWith("base64:")) {
      keyBytes = safeBase64(normalized.substring("base64:".length()), false);
    } else if (normalized.startsWith("base64url:")) {
      keyBytes = safeBase64(normalized.substring("base64url:".length()), true);
    } else if (normalized.startsWith("plain:")) {
      keyBytes = normalized.substring("plain:".length()).getBytes(StandardCharsets.UTF_8);
    } else {
      // 2) 자동 감지: 표준 Base64 -> Base64URL -> 평문
      keyBytes = tryDecodeBase64(normalized);
      if (keyBytes == null) keyBytes = tryDecodeBase64Url(normalized);
      if (keyBytes == null) keyBytes = normalized.getBytes(StandardCharsets.UTF_8);
    }

    // 3) 최소 길이 보장 (>= 32 bytes = 256 bits)
    if (keyBytes.length < 32) {
      // 개발/유연성 위해 보완: 평문/짧은 키는 SHA-256으로 256비트로 도출
      keyBytes = sha256(keyBytes);
      log.warn("JWT secret was shorter than 256 bits. Derived a 256-bit key with SHA-256.");
    }

    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessExp = accessExp;
    this.refreshExp = refreshExp;

    log.info("JWT key length = {} bits", keyBytes.length * 8);
  }

  /* ===================== 발급 ===================== */

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

  /* ===================== 파싱/검증 ===================== */

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
      return "access".equals(claims.get("typ"));
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public boolean isRefresh(String token) {
    try {
      var claims = Jwts.parserBuilder().setSigningKey(key).build()
          .parseClaimsJws(token).getBody();
      return "refresh".equals(claims.get("typ"));
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public Jws<Claims> parse(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
  }

  /* ===================== 내부 헬퍼 ===================== */

  private static String stripQuotes(String s) {
    if (s == null) return "";
    String t = s.trim();
    if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
      t = t.substring(1, t.length() - 1);
    }
    return t;
  }

  private static byte[] tryDecodeBase64(String s) {
    try {
      return Decoders.BASE64.decode(s);
    } catch (RuntimeException ignore) { // DecodingException 포함
      return null;
    }
  }

  private static byte[] tryDecodeBase64Url(String s) {
    try {
      return Decoders.BASE64URL.decode(s);
    } catch (RuntimeException ignore) {
      return null;
    }
  }

  private static byte[] safeBase64(String s, boolean url) {
    try {
      return url ? Decoders.BASE64URL.decode(s) : Decoders.BASE64.decode(s);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Invalid " + (url ? "Base64URL" : "Base64") + " JWT secret", e);
    }
  }

  private static byte[] sha256(byte[] input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return md.digest(input);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
