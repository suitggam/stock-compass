package dev.a301.stock.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtUtil jwt;
  public JwtAuthFilter(JwtUtil jwt) { this.jwt = jwt; }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    // 이미 인증된 경우 스킵
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      chain.doFilter(req, res);
      return;
    }

    String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
      chain.doFilter(req, res);
      return;
    }

    String token = auth.substring(7).trim();

    try {
      var jws = jwt.parse(token);
      Claims claims = jws.getBody();

      // access 토큰만 허용
      Object typ = claims.get("typ");
      if (!"access".equals(typ)) {
        chain.doFilter(req, res);
        return;
      }

      // ✅ uid를 우선 사용 (Number 캐스팅 주의)
      Integer userNo = toInt(claims.get("uid"));
      if (userNo == null) {
        // 호환성: subject에 userNo가 들어있는 예전 토큰도 처리
        userNo = toInt(claims.getSubject());
      }

      if (userNo != null) {
        // (선택) 이메일/닉네임을 꺼내 사용할 수 있음
        String email = claims.get("email", String.class);
        String nickname = claims.get("nickname", String.class);

        // 필요시 커스텀 Principal 클래스를 만들어 email/nickname까지 담아도 됨
        var authentication = new UsernamePasswordAuthenticationToken(
            userNo, // principal
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }

    } catch (JwtException | IllegalArgumentException e) {
      // 유효하지 않은 토큰은 무시하고 컨텍스트 비움
      logger.warn("Invalid JWT", e);
      SecurityContextHolder.clearContext();
    }

    chain.doFilter(req, res);
  }

  @Nullable
  private Integer toInt(Object v) {
    if (v == null) return null;
    if (v instanceof Integer i) return i;
    if (v instanceof Number n) return n.intValue();
    try { return Integer.valueOf(String.valueOf(v)); }
    catch (Exception ignored) { return null; }
  }
}
