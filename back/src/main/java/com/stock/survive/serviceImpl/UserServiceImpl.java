package com.stock.survive.serviceImpl;

import com.stock.survive.dto.UserSummaryDto;
import com.stock.survive.entity.OauthIdentity;
import com.stock.survive.entity.User;
import com.stock.survive.repository.UserRepository;
import com.stock.survive.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserSummaryDto changeNickname(Long userId, String raw) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        String nn = (raw == null ? "" : raw.trim());
        if (nn.length() < 2 || nn.length() > 30)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME");
        if (!nn.matches("^[a-zA-Z0-9가-힣._-]+$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME_CHAR");

        if (!nn.equals(u.getNickname())) {
            if (userRepository.existsByNickname(nn))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "NICKNAME_ALREADY_TAKEN");
            u.changeNickname(nn);                 // dirty checking
            userRepository.flush();               // 유니크 제약 즉시 검증(선택)
        }

        String avatar = u.getIdentities()==null ? null :
                u.getIdentities().stream().map(OauthIdentity::getProfileImgUrl)
                        .filter(s -> s!=null && !s.isBlank()).findFirst().orElse(null);

        return UserSummaryDto.of(u, avatar);
    }

    @Override
    public void deleteMe(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        userRepository.delete(u);
        userRepository.flush();
    }
}

