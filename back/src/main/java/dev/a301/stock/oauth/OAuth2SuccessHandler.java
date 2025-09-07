package dev.a301.stock.oauth;

import dev.a301.stock.jwt.JwtUtil;
import dev.a301.stock.service.OauthIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final OauthIdentityService linkSvc;
  private final JwtUtil jwt;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                      Authentication authentication) throws IOException {
    var token = (OAuth2AuthenticationToken) authentication;
    String provider = token.getAuthorizedClientRegistrationId(); // google | kakao
    OAuth2User p = (OAuth2User) authentication.getPrincipal();
    Map<String,Object> a = p.getAttributes();

    String providerUserId=null, email=null, nickname=null, image=null;
    boolean emailVerified=false;

    if ("google".equals(provider)) {
      providerUserId = (String) a.get("sub");
      email = (String) a.get("email");
      nickname = (String) a.getOrDefault("name", email);
      image = (String) a.get("picture");
      Object ev = a.get("email_verified");
      emailVerified = ev instanceof Boolean b ? b : "true".equals(String.valueOf(ev));
    } else { // kakao
      providerUserId = String.valueOf(a.get("id"));
      Map<String,Object> acc = (Map<String,Object>) a.get("kakao_account");
      if (acc != null) {
        email = (String) acc.get("email");
        Map<String,Object> profile = (Map<String,Object>) acc.get("profile");
        if (profile != null) {
          nickname = (String) profile.get("nickname");
          image = (String) profile.get("profile_image_url");
        }
      }
    }

    Integer userNo = linkSvc.upsert(provider, providerUserId, email, nickname, image, emailVerified);

    String access  = jwt.issueAccess(userNo);
    String refresh = jwt.issueRefresh(userNo);

    // 로컬(HTTP)에서는 Secure 금지 + Lax (리다이렉트에선 저장됨) <- 뭔뜻이당가?
    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refresh)
        .httpOnly(true)
        .secure(false)
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ofSeconds(1209600)) // 14일
        .build();
    res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

    String redirect = UriComponentsBuilder
    .fromUriString("http://localhost:5173") // 나중에 실제 우리 도메인으로 교체
    .path("/oauth/success")
    .queryParam("access", access)            
    .build(true)                             
    .toUriString();

res.sendRedirect(redirect);
  }
}
