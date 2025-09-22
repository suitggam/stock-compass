package com.stock.survive.controller;

import com.stock.survive.dto.UserSummaryDto;
import com.stock.survive.entity.OauthIdentity;
import com.stock.survive.entity.User;
import com.stock.survive.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    /** 로그인 유저 정보 */
    @GetMapping("/login-user")
    @Transactional(Transactional.TxType.SUPPORTS)
    public UserSummaryDto loginUser(Authentication auth) {
        Integer uid = extractUid(auth);
        User u = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        // ← 컬렉션 이름은 identities
        String avatarUrl = (u.getIdentities() == null) ? null :
                u.getIdentities().stream()
                        .map(OauthIdentity::getProfileImgUrl)
                        .filter(img -> img != null && !img.isBlank())
                        .findFirst()
                        .orElse(null);

        return UserSummaryDto.of(u, avatarUrl);
    }

    /** 요청 바디 */
    public record NickReq(@NotBlank String nickname) {}

    /** 닉네임 변경시 이미지 없어지는거 방지 */
    private String pickAvatarUrl(User u) {
        return (u.getIdentities() == null) ? null :
                u.getIdentities().stream()
                        .map(OauthIdentity::getProfileImgUrl) // ← 이미 맞게 쓰고 계신 getter
                        .filter(img -> img != null && !img.isBlank())
                        .findFirst()
                        .orElse(null);
    }

    /** 닉네임 변경 */
    @PatchMapping("/me")
    @Transactional
    public UserSummaryDto updateNickname(Authentication auth, @Valid @RequestBody NickReq req) {
        Integer uid = extractUid(auth);
        User u = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        String nn = req.nickname().trim();

        // 1-1) 기본 검증
        if (nn.length() < 2 || nn.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME");
        }
        // 1-2)영어,숫자,한글만
         if (!nn.matches("^[a-zA-Z0-9가-힣._-]+$")) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME_CHAR");
         }

        // 2) 동일값이면 바로 반환
        if (nn.equals(u.getNickname())) {
            return UserSummaryDto.of(u, pickAvatarUrl(u));
        }

        // 3) 중복 체크
        if (userRepository.existsByNickname(nn)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "NICKNAME_ALREADY_TAKEN");
        }

        // 4) 변경
        try {
            try {
                // setNickname(String) 우선 시도
                u.getClass().getMethod("setNickname", String.class).invoke(u, nn);
            } catch (NoSuchMethodException noSetter) {
                try {
                    // changeNickname(String) 도메인 메서드 시도
                    u.getClass().getMethod("changeNickname", String.class).invoke(u, nn);
                } catch (NoSuchMethodException noDomain) {
                    // 마지막: 필드 직접 세팅
                    var f = User.class.getDeclaredField("nickname");
                    f.setAccessible(true);
                    f.set(u, nn);
                }
            }

            userRepository.saveAndFlush(u);
        } catch (DataIntegrityViolationException e) {
            // DB 유니크 제약에 걸린 경우(동시성 등)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "NICKNAME_ALREADY_TAKEN");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CANNOT_UPDATE_NICKNAME");
        }

        return UserSummaryDto.of(u, pickAvatarUrl(u));
    }

    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<Void> deleteMe(Authentication auth) {
        Integer uid = extractUid(auth);

        User u = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        try {
            userRepository.delete(u);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {

            // FK 등으로 삭제 불가한 경우
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CANNOT_DELETE_USER_IN_USE");
        }

        return ResponseEntity.noContent().build();
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
