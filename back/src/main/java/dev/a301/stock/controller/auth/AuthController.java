package dev.a301.stock.controller.auth;

import dev.a301.stock.dto.auth.response.AuthResponse;
import dev.a301.stock.dto.user.response.UserSummaryResponse;
import dev.a301.stock.entity.auth.RefreshToken;
import dev.a301.stock.entity.user.User;
import dev.a301.stock.global.util.HashUtils;
import dev.a301.stock.repository.user.UserRepository;
import dev.a301.stock.service.auth.RefreshTokenService;
import dev.a301.stock.service.auth.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;

  // --- 쿠키 속성(프로퍼티) ---
  @Value("${app.cookie.refresh-name:refresh_token}")
  private String refreshCookieName;
  @Value("${app.cookie.same-site:None}")
  private String sameSite;      // "Lax" | "Strict" | "None"
  @Value("${app.cookie.secure:false}")
  private boolean cookieSecure; // dev=false, prod=true
  @Value("${app.cookie.path:/}")
  private String cookiePath;
  @Value("${app.cookie.max-age-days:14}")
  private long cookieMaxAgeDays;

  public record LoginRequest(String email, String nickname) {}

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req, HttpServletRequest httpReq) {
    if (req.email() == null || req.email().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email required");
    }

    // 유저 찾거나 생성
    User user = userRepository.findBySocialEmail(req.email())
        .orElseGet(() -> userRepository.save(User.builder()
            .socialEmail(req.email())
            .nickname(req.nickname() != null && !req.nickname().isBlank()
                ? req.nickname()
                : ("user_" + System.currentTimeMillis()))
            .build()));

    Integer userNo = user.getUserNo();

    // 토큰 발급 (access에는 닉네임 포함)
    String access  = tokenService.issueAccessToken(userNo, user.getNickname());
    String refresh = tokenService.issueRefreshToken(
        userNo, httpReq.getHeader("User-Agent"), clientIp(httpReq));

    // refresh -> HttpOnly 쿠키로 심기 (프로퍼티 기반)
    ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refresh)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite(sameSite)
        .path(cookiePath)
        .maxAge(Duration.ofDays(cookieMaxAgeDays))
        .build();

    var summary = new UserSummaryResponse(userNo, user.getNickname(), user.getSocialEmail());
    var body = new AuthResponse(summary, access);

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(body);
  }

  @PostMapping("/refresh")
  public ResponseEntity<Map<String, String>> refresh(
      // @CookieValue에 프로퍼티를 직접 쓰긴 어려우니, 기본 이름(=refresh_token) 유지
      @CookieValue(value = "refresh_token", required = false) String refresh
  ) {
    if (refresh == null || refresh.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no refresh cookie");
    }

    String hash = HashUtils.sha256Hex(refresh);
    RefreshToken rt = refreshTokenService.findValid(hash)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid/expired refresh"));

    String newAccess = tokenService.issueAccessToken(
        rt.getUser().getUserNo(), rt.getUser().getNickname());

    return ResponseEntity.ok(Map.of("accessToken", newAccess));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(value = "refresh_token", required = false) String refresh
  ) {
    // 세팅 때와 "동일 속성"으로 삭제해야 확실히 지워짐
    ResponseCookie del = ResponseCookie.from(refreshCookieName, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite(sameSite)
        .path(cookiePath)
        .maxAge(0)
        .build();

    if (refresh != null && !refresh.isBlank()) {
      refreshTokenService.findValid(HashUtils.sha256Hex(refresh))
          .ifPresent(refreshTokenService::revoke);
    }

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, del.toString())
        .build();
  }

  private String clientIp(HttpServletRequest req) {
    String ip = req.getHeader("X-Forwarded-For");
    if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
    return ip;
  }
}
