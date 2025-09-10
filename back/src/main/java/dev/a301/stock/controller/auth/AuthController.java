package dev.a301.stock.controller.auth;

import dev.a301.stock.entity.user.User;
import dev.a301.stock.global.util.HashUtils;
import dev.a301.stock.service.auth.RefreshTokenService;
import dev.a301.stock.service.auth.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;

  @Value("${app.cookie.refresh-name:refresh_token}")
  private String refreshCookieName;

  @Value("${app.cookie.same-site:Lax}")
  private String refreshSameSite;

  @Value("${app.cookie.secure:false}")
  private boolean refreshSecure;

  @Value("${app.cookie.max-age-days:14}")
  private long refreshMaxAgeDays;

  /** 액세스 재발급 */
  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(HttpServletRequest req, HttpServletResponse res) {
    String raw = resolveRefreshFromCookies(req);
    if (raw == null) {
      log.warn("[REFRESH] no refresh cookie");
      addCookieDeletes(res);
      return ResponseEntity.status(401).body(Map.of("message", "no refresh cookie"));
    }

    try {
      User user = refreshTokenService.consumeAndGetUser(raw);

      String access = tokenService.issueAccessToken(user.getUserNo(), user.getNickname());
      String newRefresh = tokenService.issueRefreshToken(
          user.getUserNo(),
          req.getHeader("User-Agent"),
          clientIp(req)
      );

      addCookie(res, newRefresh);
      addCookieDeletes(res); // 혹시 남은 변종 정리

      return ResponseEntity.ok(Map.of("accessToken", access));

    } catch (IllegalStateException ex) {
      log.warn("[REFRESH] invalid/expired: {}", ex.getMessage());
      addCookieDeletes(res);
      return ResponseEntity.status(401).body(Map.of("message", "invalid/expired refresh"));
    }
  }

  /** 로그아웃 */
  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest req, HttpServletResponse res) {
    String raw = resolveRefreshFromCookies(req);
    refreshTokenService.deleteOnLogout(raw);
    addCookieDeletes(res);
    return ResponseEntity.noContent().build();
  }

  /* ---------- helpers ---------- */

  /** 같은 이름의 쿠키가 여러 개 있으면, DB에 존재하는 해시와 매칭되는 값을 선택 */
  private String resolveRefreshFromCookies(HttpServletRequest req) {
    Cookie[] cookies = req.getCookies();
    if (cookies == null || cookies.length == 0) return null;

    List<String> candidates = new ArrayList<>();
    for (Cookie c : cookies) {
      if (refreshCookieName.equals(c.getName())) {
        String v = c.getValue();
        if (v != null && !v.isBlank()) candidates.add(v);
      }
    }
    if (candidates.isEmpty()) return null;

    // 디버깅: 후보 목록 로깅 + DB 존재여부
    for (String v : candidates) {
      String hp = prefix(HashUtils.sha256Hex(v));
      boolean exists = refreshTokenService.findValid(HashUtils.sha256Hex(v)).isPresent();
      log.info("[REFRESH] candidate raw.prefix={} hash.prefix={} inDB={}", prefix(v), hp, exists);
    }

    // 1) DB에 있는 해시와 일치하는 후보 우선 선택
    for (String v : candidates) {
      if (refreshTokenService.findValid(HashUtils.sha256Hex(v)).isPresent()) {
        log.info("[REFRESH] picked DB-matched cookie prefix={}", prefix(v));
        return v;
      }
    }
    // 2) 없으면 첫 번째(결국 401 처리)
    log.info("[REFRESH] fall back to first cookie prefix={}", prefix(candidates.get(0)));
    return candidates.get(0);
  }

  private void addCookie(HttpServletResponse res, String raw) {
    ResponseCookie cookie = ResponseCookie.from(refreshCookieName, raw)
        .httpOnly(true)
        .secure(refreshSecure)
        .sameSite(refreshSameSite)
        .path("/")
        .maxAge(Duration.ofDays(refreshMaxAgeDays))
        .build();
    res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  /** 다양한 경로/도메인의 예전 쿠키 제거(클라이언트가 여러 개를 보내는 현상 방지) */
  private void addCookieDeletes(HttpServletResponse res) {
    String[] paths = { "/", "/api", "/api/auth", "/users" };
    String[] domains = { null, "localhost", "127.0.0.1" };

    for (String p : paths) {
      for (String d : domains) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(refreshCookieName, "")
            .httpOnly(true)
            .secure(refreshSecure)
            .sameSite(refreshSameSite)
            .path(p)
            .maxAge(0);
        if (d != null) b.domain(d);
        res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
      }
    }
  }

  private static String clientIp(HttpServletRequest req) {
    String ip = req.getHeader("X-Forwarded-For");
    return (ip == null || ip.isBlank()) ? req.getRemoteAddr() : ip;
  }

  private static String prefix(String s) {
    if (s == null) return "null";
    return s.length() <= 10 ? s : s.substring(0, 10);
  }
}
