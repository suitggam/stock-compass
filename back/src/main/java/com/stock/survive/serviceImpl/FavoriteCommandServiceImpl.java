package com.stock.survive.serviceImpl;

import com.stock.survive.entity.StockItems;
import com.stock.survive.entity.User;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.repository.UserRepository;
import com.stock.survive.service.FavoriteCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteCommandServiceImpl implements FavoriteCommandService {
    private final UserRepository userRepository;
    private final StockItemsRepository stockItemsRepository;

    @Override
    public boolean toggleFavorite(Long userId, Long itemNo) {
        User u = userRepository.findWithFavoritesById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        StockItems item = stockItemsRepository.findById(itemNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND"));

        Set<StockItems> favs = u.getFavorites();
        if (favs.contains(item)) { favs.remove(item); return false; }
        else { favs.add(item); return true; }
    }
}
