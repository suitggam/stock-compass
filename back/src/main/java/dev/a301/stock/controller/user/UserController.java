// src/main/java/dev/a301/stock/modules/user/web/UserController.java
package dev.a301.stock.controller.user;

import dev.a301.stock.dto.user.response.UserProfileResponse;
import dev.a301.stock.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserRepository userRepository;

  private int currentUserNo(Authentication auth) {
    Object p = auth.getPrincipal();
    if (p instanceof Integer i) return i;
    if (p instanceof String s) return Integer.parseInt(s);
    throw new IllegalStateException("Unexpected principal: " + p);
  }

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserProfileResponse> me(Authentication auth) {
    int userNo = currentUserNo(auth);
    var dto = userRepository.findProfileByUserNo(userNo)
      .orElseThrow(() -> new IllegalStateException("User not found: " + userNo));
    return ResponseEntity.ok(dto);
  }

  @GetMapping("/check-nickname")
  public ResponseEntity<?> checkNickname(@RequestParam("value") String value) {
    boolean taken = userRepository.existsByNickname(value);
    return ResponseEntity.ok(java.util.Map.of("available", !taken));
  }
}
