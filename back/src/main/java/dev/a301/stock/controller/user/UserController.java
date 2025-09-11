package dev.a301.stock.controller.user;

import dev.a301.stock.dto.user.response.UserProfileResponse;
import dev.a301.stock.entity.user.User;
import dev.a301.stock.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

  // 내 프로필 조회
  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserProfileResponse> me(Authentication auth) {
    int userNo = currentUserNo(auth);
    var dto = userRepository.findProfileByUserNo(userNo)
        .orElseThrow(() -> new IllegalStateException("User not found: " + userNo));
    return ResponseEntity.ok(dto);
  }

  // 닉네임 중복 확인 (비로그인도 가능하게 열어둠)
  // 프런트: GET /api/users/check-nickname?nickname=xxx  -> { "available": true/false }
  @GetMapping("/check-nickname")
  public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam("nickname") String nickname) {
    boolean taken = userRepository.existsByNickname(nickname);
    return ResponseEntity.ok(Map.of("available", !taken));
  }

  // 내 닉네임 변경 (로그인 필요)
  // 프런트: PATCH /api/users/me/nickname  { "nickname": "새닉네임" }
  public record NicknameRequest(String nickname) {}

  @PatchMapping("/me/nickname")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> changeNickname(Authentication auth, @RequestBody NicknameRequest req) {
    String nn = req.nickname();
    // 형식 검증 (서비스 정책에 맞게 조정)
    if (nn == null || nn.trim().length() < 2 || nn.length() > 30 ||
        !nn.matches("^[\\w가-힣\\- ]+$")) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    int userNo = currentUserNo(auth);
    User u = userRepository.findById(userNo).orElseThrow();

    // 동일 닉네임이면 변경 없음
    if (nn.equals(u.getNickname())) {
      return ResponseEntity.noContent().build();
    }

    // 중복 체크
    if (userRepository.existsByNickname(nn)) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409
    }

    u.setNickname(nn);
    userRepository.save(u);
    return ResponseEntity.noContent().build(); // 204
  }
}