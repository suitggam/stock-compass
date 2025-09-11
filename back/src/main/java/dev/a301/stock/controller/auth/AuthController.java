package dev.a301.stock.controller.auth;

import dev.a301.stock.dto.auth.response.AuthResponse;
import dev.a301.stock.global.util.HashUtils;
import dev.a301.stock.service.auth.RefreshTokenService;
import dev.a301.stock.service.auth.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final RefreshTokenService refreshTokenService;
  private final TokenService tokenService;

  /* ===== 쿠키 설정 (OAuth2SuccessHandler와 동일 값 사용) ===== */
  @Value("${app.cookie.refresh-name:refresh_token}")
  private String refreshCookieName;
  @Value("${app.cookie.same-site:Lax}")
  private String refreshSameSite;           // 분리 도메인이면 None(+secure=true)
  @Value("${app.cookie.secure:false}")
  private boolean refreshSecure;
  @Value("${app.cookie.max-age-days:14}")
  private long refreshMaxAgeDays;

  /** 리프레시를 1회용으로 소비하고, Access 발급 + 새 리프레시로 회전 */
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
    List<String> candidates = extractRefreshCookies(request);
    if (candidates.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, null));
    }

    for (String raw : candidates) {
      String hash = HashUtils.sha256Hex(raw);

      var userNoOpt = refreshTokenService.consumeByHash(hash); // Optional<Integer>, 소비 시 DB에서 삭제
      if (userNoOpt.isPresent()) {
        Integer userNo = userNoOpt.get();

        // 1) Access 발급
        String access = tokenService.issueAccessTokenByUserId(userNo);

        // 2) RT 회전(새 RT 발급 + 저장) → HttpOnly 쿠키로 내려줌
        String newRefresh = tokenService.issueRefreshToken(
            userNo,
            request.getHeader("User-Agent"),
            clientIp(request)
        );
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, newRefresh)
            .httpOnly(true)
            .secure(refreshSecure)
            .sameSite(refreshSameSite)
            .path("/")
            .maxAge(Duration.ofDays(refreshMaxAgeDays))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("[REFRESH] ok uid={} (rotated)", userNo);
        return ResponseEntity.ok(new AuthResponse(null, access));
      }
    }

    log.warn("[REFRESH] invalid/expired candidates={}", candidates.size());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, null));
  }

  /** 안전한 로그아웃: 유효/무효 RT 상관없이 베스트에포트로 소비 시도, 쿠키는 전부 제거, 204 */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    try {
      for (String raw : extractRefreshCookies(request)) {
        try {
          refreshTokenService.consumeIfValid(raw); // 없어도/만료여도 예외 무시
        } catch (Exception ignore) {
          log.debug("[LOGOUT] ignore consume error: {}", ignore.toString());
        }
      }
    } finally {
      clearRefreshCookies(response); // 항상 쿠키 제거
    }
    return ResponseEntity.noContent().build();
  }

  /* ================= helpers ================= */

  private List<String> extractRefreshCookies(HttpServletRequest request) {
    Cookie[] arr = request.getCookies();
    if (arr == null || arr.length == 0) return List.of();
    return Arrays.stream(arr)
        .filter(c -> refreshCookieName.equals(c.getName()))
        .map(Cookie::getValue)
        .filter(v -> v != null && !v.isBlank())
        .distinct()
        .collect(Collectors.toList());
  }

  /** 다양한 경로/도메인 조합으로 삭제 쿠키 발급 (개발 환경 흔적 정리) */
  private void clearRefreshCookies(HttpServletResponse res) {
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
    log.info("[LOGOUT] cleared refresh cookies");
  }

  private String clientIp(HttpServletRequest req) {
    String ip = req.getHeader("X-Forwarded-For");
    if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
    return ip;
  }
}
