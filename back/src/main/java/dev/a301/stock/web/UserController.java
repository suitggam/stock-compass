package dev.a301.stock.web;

import dev.a301.stock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

  private final UserRepository userRepo;

  private Integer userNoOf(Principal principal) {
    if (principal == null) return null;
    try { return Integer.valueOf(principal.getName()); }
    catch (NumberFormatException e) { return null; }
  }

  @GetMapping("/login-user")
  public ResponseEntity<?> me(Principal principal) {
    Integer userNo = userNoOf(principal);
    if (userNo == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

    return userRepo.findLoginUserDtoById(userNo)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @PatchMapping("/me")
  public ResponseEntity<?> updateNickname(Principal principal, @RequestParam String nickname) {
    Integer userNo = userNoOf(principal);
    if (userNo == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

    return userRepo.findById(userNo)
        .map(u -> { u.setNickname(nickname); userRepo.save(u); return ResponseEntity.ok().build(); })
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }
}