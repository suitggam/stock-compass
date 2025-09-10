package dev.a301.stock.global.security.oauth;

import dev.a301.stock.entity.user.OauthIdentity;
import dev.a301.stock.entity.user.User;
import dev.a301.stock.global.security.jwt.JwtUtil;
import dev.a301.stock.global.util.HashUtils;
import dev.a301.stock.service.auth.RefreshTokenService;
import dev.a301.stock.repository.user.UserRepository;
import dev.a301.stock.service.user.OauthIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
  private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

  private final UserRepository userRepository;
  private final OauthIdentityService oauthIdentityService;
  private final JwtUtil jwtUtil;
  private final RefreshTokenService refreshTokenService; // ★ 추가

  @Value("${app.oauth2.redirect-success:http://localhost:5173/oauth/success}")
  private String redirectSuccessUrl;

  @Value("${app.oauth2.redirect-fail:http://localhost:5173/oauth/fail}")
  private String redirectFailUrl;

  @Override
  @Transactional
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
    try {
      OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
      OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

      String registrationId = token.getAuthorizedClientRegistrationId(); // google | kakao
      OauthIdentity.Provider provider;
      String providerUserId;
      String email = null;
      String picture = null;
      boolean emailVerified = false;

      if ("google".equalsIgnoreCase(registrationId)) {
        provider = OauthIdentity.Provider.GOOGLE;
        providerUserId = toStr(oAuth2User.getAttribute("sub"));
        email         = toStr(oAuth2User.getAttribute("email"));
        picture       = toStr(oAuth2User.getAttribute("picture"));
        emailVerified = toBool(oAuth2User.getAttribute("email_verified"));
        if (!StringUtils.hasText(providerUserId)) { sendFail(response, "google_no_sub"); return; }
      } else {
        provider = OauthIdentity.Provider.KAKAO;
        providerUserId = toStr(oAuth2User.getAttribute("id"));
        Map<String, Object> kakaoAccount = toMap(oAuth2User.getAttribute("kakao_account"));
        if (kakaoAccount != null) {
          email = toStr(kakaoAccount.get("email"));
          emailVerified = toBool(kakaoAccount.get("is_email_verified")) || toBool(kakaoAccount.get("is_email_valid"));
          Map<String, Object> profile = toMap(kakaoAccount.get("profile"));
          if (profile != null) {
            picture = toStr(profile.get("profile_image_url"));
            if (!StringUtils.hasText(picture)) picture = toStr(profile.get("thumbnail_image_url"));
          }
        }
        if (!StringUtils.hasText(email)) email = "kakao_" + providerUserId + "@oauth.local"; // 스키마 NOT NULL 보장
        if (!StringUtils.hasText(providerUserId)) { sendFail(response, "kakao_no_id"); return; }
      }

      // 사용자 연동/생성
      OauthIdentity existing = oauthIdentityService.find(provider, providerUserId);
      User user;
      if (existing != null) {
        user = existing.getUser();
      } else {
        Optional<User> found = userRepository.findBySocialEmail(email);
        if (found.isPresent()) {
          user = found.get();
        } else {
          String nickname = generateUniqueNickname(email);
          user = userRepository.saveAndFlush(
              User.builder()
                  .socialEmail(email)
                  .nickname(nickname)
                  .cancel(false)
                  .build()
          );
        }
        oauthIdentityService.link(user, provider, providerUserId, email, picture, emailVerified);
      }

      // ★ JWT 발급 (access는 최소 claim, refresh는 PII 제거)
      String accessToken  = jwtUtil.issueAccess(user.getUserNo(), user.getNickname());
      String refreshToken = jwtUtil.issueRefresh(user.getUserNo());

      // ★ refresh 토큰 DB에는 해시로 저장 (UA/IP 저장 가능)
      String ua  = request.getHeader("User-Agent");
      String ip  = clientIp(request);
      String hash = HashUtils.sha256Hex(refreshToken);
      refreshTokenService.save(user.getUserNo(), hash,
          jwtUtil.getExpiry(refreshToken).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
          ua, ip);

      // ★ HttpOnly 쿠키로 refresh 전송
      ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
          .httpOnly(true).secure(true).sameSite("Strict")
          .path("/api/auth") // /refresh, /logout의 경로
          .maxAge(Duration.ofDays(14))
          .build();
      response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

      // ★ URL에 토큰 넣지 말고(로그/히스토리 노출), FE가 /api/auth/refresh로 access 받도록
      String json = """
          {"userNo":%d,"socialEmail":"%s","nickname":"%s"}
          """.formatted(user.getUserNo(), esc(user.getSocialEmail()), esc(user.getNickname())).trim();

      String payload = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(json.getBytes(StandardCharsets.UTF_8));

      String target = UriComponentsBuilder.fromUriString(redirectSuccessUrl)
          .queryParam("payload", payload) // 토큰 없음
          .build(true)
          .toUriString();

      log.info("[OAUTH-SUCCESS] provider={} userNo={} email={} → {}", registrationId, user.getUserNo(), user.getSocialEmail(), target);
      response.sendRedirect(target);

    } catch (Exception ex) {
      log.error("[OAUTH-ERROR]", ex);
      sendFail(response, "server_error");
    }
  }

  // helpers
  private void sendFail(HttpServletResponse response, String reason) throws IOException {
    String fail = UriComponentsBuilder.fromUriString(redirectFailUrl)
        .queryParam("reason", reason)
        .build(true)
        .toUriString();
    response.sendRedirect(fail);
  }

  private static String toStr(Object v) { return v == null ? null : String.valueOf(v); }
  private static boolean toBool(Object v) {
    if (v instanceof Boolean b) return b;
    if (v == null) return false;
    String s = String.valueOf(v);
    return "true".equalsIgnoreCase(s) || "1".equals(s);
  }
  @SuppressWarnings("unchecked")
  private static Map<String, Object> toMap(Object o) { return (o instanceof Map<?, ?> m) ? (Map<String, Object>) m : null; }

  private String generateUniqueNickname(String seed) {
    String base = (seed == null) ? "user" : seed.split("@")[0].replaceAll("[^a-zA-Z0-9_\\-]", "");
    if (base.isBlank()) base = "user";
    if (base.length() > 20) base = base.substring(0, 20);
    String candidate = base;
    Random r = new Random();
    while (userRepository.existsByNickname(candidate)) {
      candidate = base + "_" + (1000 + r.nextInt(9000));
      if (candidate.length() > 30) candidate = candidate.substring(0, 30);
    }
    return candidate;
  }

  private static String esc(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
  private String clientIp(HttpServletRequest req) {
    String ip = req.getHeader("X-Forwarded-For");
    if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
    return ip;
  }
}
