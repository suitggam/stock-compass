package com.stock.survive.controller;

import com.stock.survive.dto.UserSummaryDto;
import com.stock.survive.entity.User;
import com.stock.survive.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    /** 로그인한 유저의 요약 정보 (프론트: GET /users/login-user 호출) */
    @GetMapping("/login-user")
    public UserSummaryDto loginUser(Authentication auth) {
        Integer uid = extractUid(auth);
        User u = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        return UserSummaryDto.of(u);
    }

    /** 닉네임 변경(예시) */
    public record NickReq(@NotBlank String nickname) {}
    @PatchMapping("/me")
    public UserSummaryDto updateNickname(Authentication auth, @RequestBody NickReq req) {
        Integer uid = extractUid(auth);
        User u = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        // 간단 검증
        String nn = req.nickname().trim();
        if (nn.length() < 2 || nn.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME");
        }
        // 엔티티가 @Getter만 있으면 빌더/세터가 없으니, 필요 시 엔티티에 닉네임 변경 메서드 추가하세요.
        // 예: u.changeNickname(nn);
        // 여기선 JPA 업데이트를 위해 리플렉션/세터가 필요 -> 프로젝트 스타일에 맞게 구현
        try {
            var f = User.class.getDeclaredField("nickname");
            f.setAccessible(true);
            f.set(u, nn);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CANNOT_UPDATE_NICKNAME");
        }
        userRepository.save(u);
        return UserSummaryDto.of(u);
    }

    /** JWTCheckFilter가 principal에 uid를 넣어줬다는 가정 하에 추출 */
    private Integer extractUid(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        Object p = auth.getPrincipal();
        if (p instanceof Integer i) return i;
        if (p instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(p)); } catch (Exception ignored) {}
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_PRINCIPAL");
    }
}
