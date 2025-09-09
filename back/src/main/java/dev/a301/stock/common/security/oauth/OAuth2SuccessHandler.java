package dev.a301.stock.common.security.oauth;

import dev.a301.stock.common.security.jwt.JwtUtil;
import dev.a301.stock.modules.user.domain.OauthIdentity;
import dev.a301.stock.modules.user.domain.User;
import dev.a301.stock.modules.user.repository.UserRepository;
import dev.a301.stock.modules.user.service.OauthIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.sql.SQLIntegrityConstraintViolationException;
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

      // 원본 attribute 로그로 모양 확인
      log.debug("[OAUTH] provider={} rawAttributes={}", token.getAuthorizedClientRegistrationId(), oAuth2User.getAttributes());

      // --- 1) 프로바이더별 프로필 파싱 (초강경) ------------------------------
      String registrationId = token.getAuthorizedClientRegistrationId(); // google | kakao
      OauthIdentity.Provider provider;
      String providerUserId;
      String email = null;
      String picture = null;
      boolean emailVerified = false;

      if ("google".equalsIgnoreCase(registrationId)) {
        provider = OauthIdentity.Provider.GOOGLE;
        providerUserId = toStr(oAuth2User.getAttribute("sub"));         // 필수
        email         = toStr(oAuth2User.getAttribute("email"));        // 선택(스코프 의존)
        picture       = toStr(oAuth2User.getAttribute("picture"));      // 선택
        emailVerified = toBool(oAuth2User.getAttribute("email_verified"));

        // 구글은 sub 없으면 말이 안 됨 → 명확히 실패
        if (!StringUtils.hasText(providerUserId)) {
          sendFail(response, "google_no_sub"); return;
        }

      } else {
        provider = OauthIdentity.Provider.KAKAO;
        providerUserId = toStr(oAuth2User.getAttribute("id"));          // Long일 수 있음 → 문자열화

        Map<String, Object> kakaoAccount = toMap(oAuth2User.getAttribute("kakao_account"));
        if (kakaoAccount != null) {
          email = toStr(kakaoAccount.get("email"));                     // 동의/스코프 없어 null 가능
          // 둘 다 체크
          emailVerified = toBool(kakaoAccount.get("is_email_verified")) || toBool(kakaoAccount.get("is_email_valid"));
          Map<String, Object> profile = toMap(kakaoAccount.get("profile"));
          if (profile != null) {
            picture = toStr(profile.get("profile_image_url"));
            if (!StringUtils.hasText(picture)) picture = toStr(profile.get("thumbnail_image_url"));
          }
        }
        // 이메일 미제공시 더미 부여(스키마 NOT NULL 보장)
        if (!StringUtils.hasText(email)) {
          email = "kakao_" + providerUserId + "@oauth.local";
        }
        if (!StringUtils.hasText(providerUserId)) {
          sendFail(response, "kakao_no_id"); return;
        }
      }

      log.debug("[OAUTH] parsed provider={}, providerUserId={}, email={}, emailVerified={}, picture={}",
          registrationId, providerUserId, email, emailVerified, picture);

      // --- 2) 사용자 연결/생성 ----------------------------------------------
      OauthIdentity existing = oauthIdentityService.find(provider, providerUserId);
      User user;
      if (existing != null) {
        user = existing.getUser();
        log.debug("[OAUTH] identity exists → userNo={}", user.getUserNo());
      } else {
        Optional<User> found = userRepository.findBySocialEmail(email);
        if (found.isPresent()) {
          user = found.get();
          log.debug("[OAUTH] user exists by email → userNo={}", user.getUserNo());
        } else {
          String nickname = generateUniqueNickname(email);
          user = userRepository.saveAndFlush(
              User.builder()
                  .socialEmail(email)
                  .nickname(nickname)
                  .cancel(false)
                  .build()
          );
          log.debug("[OAUTH] user created → userNo={}, nickname={}", user.getUserNo(), user.getNickname());
        }
        oauthIdentityService.link(user, provider, providerUserId, email, picture, emailVerified);
        log.debug("[OAUTH] identity linked (provider={}, providerUserId={})", provider, providerUserId);
      }

      // --- 3) JWT 발급 ------------------------------------------------------
      String accessToken  = jwtUtil.generateAccessToken(user.getUserNo(), user.getSocialEmail(), user.getNickname());
      String refreshToken = jwtUtil.generateRefreshToken(user.getUserNo(), user.getSocialEmail());
      log.debug("[OAUTH] jwt issued: access(len={}), refresh(len={})", len(accessToken), len(refreshToken));

      // --- 4) payload(Base64URL) 구성 --------------------------------------
      String json = """
          {"userNo":%d,"socialEmail":"%s","nickname":"%s","accessToken":"%s","refreshToken":"%s"}
          """.formatted(user.getUserNo(), esc(user.getSocialEmail()), esc(user.getNickname()), esc(accessToken), esc(refreshToken)).trim();

      String payload = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(json.getBytes(StandardCharsets.UTF_8));

      String target = UriComponentsBuilder.fromUriString(redirectSuccessUrl)
          .queryParam("payload", payload)
          .queryParam("access", accessToken)
          .queryParam("refresh", refreshToken)
          .build(true)
          .toUriString();

      log.info("[OAUTH-SUCCESS] provider={} userNo={} email={} → {}", registrationId, user.getUserNo(), user.getSocialEmail(), target);
      response.sendRedirect(target);

    } catch (Exception ex) {
      String reason = classifyReason(ex);
      Throwable root = root(ex);
      log.error("[OAUTH-ERROR] reason={} type={} msg={} rootType={} rootMsg={}",
          reason, ex.getClass().getName(), msg(ex), root.getClass().getName(), msg(root), ex);
      sendFail(response, reason);
    }
  }

  // ---------------- helpers ----------------

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
  private static Map<String, Object> toMap(Object o) {
    return (o instanceof Map<?, ?> m) ? (Map<String, Object>) m : null;
  }

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

  private static int len(String s) { return s == null ? 0 : s.length(); }

  private static Throwable root(Throwable t) { while (t.getCause()!=null && t.getCause()!=t) t=t.getCause(); return t; }
  private static String msg(Throwable t) { return (t==null||t.getMessage()==null) ? "" : t.getMessage(); }

  private String classifyReason(Throwable t) {
    Throwable r = root(t);
    String m = msg(r);
    if (r instanceof DataIntegrityViolationException || r instanceof SQLIntegrityConstraintViolationException) {
      if (contains(m, "uq_provider_subject")) return "identity_duplicate";
      if (contains(m, "uq_users_nickname"))   return "nickname_duplicate";
      if (contains(m, "uq_users_email"))      return "email_duplicate";
      if (contains(m, "Duplicate entry"))     return "db_duplicate";
      return "db_integrity";
    }
    if (r instanceof NullPointerException) return "npe";
    if (r instanceof IllegalArgumentException || r instanceof ClassCastException) return "bad_profile"; // ← 여기가 뜨던 곳
    if (r.getClass().getName().startsWith("io.jsonwebtoken")) return "jwt_error";
    return "server_error";
  }
  private static boolean contains(String h, String n) { return h!=null && n!=null && h.toLowerCase().contains(n.toLowerCase()); }
}
