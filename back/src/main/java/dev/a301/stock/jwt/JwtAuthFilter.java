package dev.a301.stock.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
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

    String auth = req.getHeader(HttpHeaders.AUTHORIZATION);

    if (auth != null && auth.startsWith("Bearer ")) {
      String token = auth.substring(7).trim();
      try {
        var jws = jwt.parse(token);
        var claims = jws.getBody();

        if ("access".equals(claims.get("typ"))) {
          Integer userNo = Integer.valueOf(claims.getSubject());
          var authentication = new UsernamePasswordAuthenticationToken(
              userNo, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
          );
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      } catch (JwtException | IllegalArgumentException e) {
        // 여기서 throw 하지 말 것 — 500 방지
        logger.warn("Invalid JWT", e);          
        SecurityContextHolder.clearContext();
      }
    }

    chain.doFilter(req, res);
  }
}
