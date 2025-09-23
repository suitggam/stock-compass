package com.stock.survive.serviceImpl;

import com.stock.survive.dto.UserSummaryDto;
import com.stock.survive.entity.OauthIdentity;
import com.stock.survive.entity.User;
import com.stock.survive.repository.UserRepository;
import com.stock.survive.service.AuthQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryServiceImpl implements AuthQueryService {

    private final UserRepository userRepository;

    @Override
    public UserSummaryDto me(Integer userId) {
        User u = userRepository.findWithIdentitiesById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "USER_NOT_FOUND"));
        String avatar = pickAvatarUrl(u);
        return UserSummaryDto.of(u, avatar);
    }

    private String pickAvatarUrl(User u) {
        return u.getIdentities() == null ? null :
                u.getIdentities().stream()
                        .map(OauthIdentity::getProfileImgUrl)
                        .filter(s -> s != null && !s.isBlank())
                        .findFirst()
                        .orElse(null);
    }
}