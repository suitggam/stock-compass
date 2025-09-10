package dev.a301.stock.controller.auth;

import dev.a301.stock.dto.auth.response.AuthResponse;
import dev.a301.stock.dto.user.response.UserSummaryResponse;
import dev.a301.stock.entity.user.User;
import dev.a301.stock.repository.user.UserRepository;
import dev.a301.stock.service.auth.RefreshTokenService;
import dev.a301.stock.service.auth.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserRepository userRepository;
  private final TokenService tokenService;              // 발급+저장 책임 집중
  private final RefreshTokenService refreshTokenService; // consume / logout 삭제 책임

  // --- 쿠키 속성(프로퍼티) ---
  @Value("${app.cookie.refresh-name:refresh_token}")
  private String refreshCookieName;
  @Value("${app.cookie.same-site:Lax}")   // dev: Lax, 배포: None(HTTPS에서 secure=true)
  private String sameSite;                // "Lax" | "Strict" | "None"
  @Value("${app.cookie.secure:false}")
  private boolean cookieSecure;           // dev=false, prod=true
  @Value("${app.cookie.path:/}")
  private String cookiePath;
  @Value("${app.cookie.max-age-days:14}")
  private long cookieMaxAgeDays;
  @Value("${app.cookie.session:false}")   // 세션쿠키 원하면 true
  private boolean cookieSession;

  public record LoginRequest(String email, String nickname) {}

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req, HttpServletRequest httpReq) {
    if (!StringUtils.hasText(req.email())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email required");
    }

    // 유저 찾거나 생성
    User user = userRepository.findBySocialEmail(req.email())
        .orElseGet(() -> userRepository.save(
            User.builder()
                .socialEmail(req.email())
                .nickname(StringUtils.hasText(req.nickname()) ? req.nickname() : ("user_" + System.currentTimeMillis()))
                .build()
        ));

    Integer userNo = user.getUserNo();

    // ★ access / refresh 발급 (refresh는 TokenService가 DB에도 저장)
    String access  = tokenService.issueAccessToken(userNo, user.getNickname());
    String refresh = tokenService.issueRefreshToken(userNo, ua(httpReq), ip(httpReq));

    // ★ 쿠키로 전달 (컨트롤러는 저장 안함!)
    ResponseCookie cookie = buildRefreshCookie(refresh);
    var summary = new UserSummaryResponse(userNo, user.getNickname(), user.getSocialEmail());
    var body = new AuthResponse(summary, access);

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(body);
  }

  /**
   * 회전(rotate):
   * 1) 기존 refresh 유효성 확인 + 하드삭제(consume)
   * 2) 새 access / refresh 발급 (발급 시 DB 저장은 TokenService가 수행)
   * 3) 새 refresh 쿠키로 교체
   */
  @PostMapping("/refresh")
  public ResponseEntity<Map<String, String>> refresh(
      @CookieValue(value = "refresh_token", required = false) String refresh,
      HttpServletRequest req
  ) {
    if (!StringUtils.hasText(refresh)) {
      return unauthorizedAndDeleteCookie("no refresh cookie");
    }

    try {
      // 1) 기존 토큰 consume(유효성 검사 + 하드 삭제) → 유저 반환
      User user = refreshTokenService.consumeAndGetUser(refresh);

      // 2) 새 토큰 발급(저장은 TokenService 내부에서 처리)
      String newAccess  = tokenService.issueAccessToken(user.getUserNo(), user.getNickname());
      String newRefresh = tokenService.issueRefreshToken(user.getUserNo(), ua(req), ip(req));

      // 3) 쿠키 교체
      ResponseCookie cookie = buildRefreshCookie(newRefresh);
      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, cookie.toString())
          .body(Map.of("accessToken", newAccess));

    } catch (Exception e) {
      // DB에 없거나 만료/경합 등 → 쿠키 정리 + 401
      return unauthorizedAndDeleteCookie("invalid/expired refresh");
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@CookieValue(value = "refresh_token", required = false) String refresh) {
    // DB에서 해당 refresh 하드 삭제(있으면)
    refreshTokenService.deleteOnLogout(refresh);
    // 브라우저 쿠키 삭제
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString())
        .build();
  }

  // ---------------- helpers ----------------

  private ResponseCookie buildRefreshCookie(String value) {
    ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(refreshCookieName, value)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite(sameSite)
        .path(cookiePath);
    if (!cookieSession) {
      b.maxAge(Duration.ofDays(cookieMaxAgeDays));
    }
    return b.build();
  }

  private ResponseCookie deleteRefreshCookie() {
    return ResponseCookie.from(refreshCookieName, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite(sameSite)
        .path(cookiePath)
        .maxAge(0)
        .build();
  }

  private ResponseEntity<Map<String, String>> unauthorizedAndDeleteCookie(String msg) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString())
        .body(Map.of("message", msg));
  }

  private static String ua(HttpServletRequest req) {
    return req.getHeader("User-Agent");
  }

  private static String ip(HttpServletRequest req) {
    String ip = req.getHeader("X-Forwarded-For");
    if (!StringUtils.hasText(ip)) ip = req.getRemoteAddr();
    return ip;
  }
}
