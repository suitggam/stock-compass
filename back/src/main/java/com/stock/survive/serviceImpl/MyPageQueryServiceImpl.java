package com.stock.survive.serviceImpl;

import com.stock.survive.dto.MyPageDto;
import com.stock.survive.entity.OauthIdentity;
import com.stock.survive.entity.User;
import com.stock.survive.repository.UserRepository;
import com.stock.survive.service.MyPageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageQueryServiceImpl implements MyPageQueryService {

    private final UserRepository userRepository;

    @Override
    public MyPageDto getMyPage(Integer userId) {
        User u = userRepository.findWithFavoritesById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "USER_NOT_FOUND"));

        String avatar = pickAvatarUrl(u);

        var favs = u.getFavorites().stream()
                .map(si -> new MyPageDto.FavoriteItemDto(si.getItemNo(), si.getCompanyName()))
                .toList();

        return MyPageDto.ofWithFavorites(u, avatar, favs);
    }

    @Override
    public Set<Integer> getFavoriteIdSet(Integer userId) {
        return new HashSet<>(userRepository.findFavoriteItemIds(userId));
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
