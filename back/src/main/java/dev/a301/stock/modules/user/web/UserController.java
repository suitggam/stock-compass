package dev.a301.stock.modules.user.web;

import dev.a301.stock.modules.user.domain.User;
import dev.a301.stock.modules.user.repository.UserRepository;
import dev.a301.stock.modules.user.web.dto.LoginUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserRepository userRepository;

  // principal 에서 userNo 꺼내기 (JwtAuthFilter가 Integer로 넣음)
  private int currentUserNo(Authentication auth) {
    Object p = auth.getPrincipal();
    if (p instanceof Integer i) return i;
    if (p instanceof String s) return Integer.parseInt(s);
    throw new IllegalStateException("Unexpected principal: " + p);
  }

  @GetMapping("/check-nickname")
  public ResponseEntity<Map<String, Object>> checkNickname(@RequestParam("value") String value) {
    boolean taken = userRepository.existsByNickname(value);
    return ResponseEntity.ok(Map.of("available", !taken));
  }

  @PutMapping("/nickname")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> updateNickname(Authentication auth, @RequestBody Map<String, String> body) {
    String newNick = body.get("nickname");
    if (newNick == null || newNick.isBlank() || newNick.length() > 30) {
      return ResponseEntity.badRequest().body(Map.of("message", "닉네임은 1~30자"));
    }
    if (userRepository.existsByNickname(newNick)) {
      return ResponseEntity.status(409).body(Map.of("message", "이미 사용 중인 닉네임"));
    }

    int userNo = currentUserNo(auth);
    User u = userRepository.findById(userNo).orElseThrow();
    u.setNickname(newNick);
    userRepository.save(u);

    return ResponseEntity.ok(Map.of("nickname", newNick));
  }

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<LoginUserDto> me(Authentication auth) {
    int userNo = currentUserNo(auth);
    User u = userRepository.findById(userNo).orElseThrow();
    return ResponseEntity.ok(LoginUserDto.builder()
        .userNo(u.getUserNo())
        .socialEmail(u.getSocialEmail())
        .nickname(u.getNickname())
        .build());
  }
}