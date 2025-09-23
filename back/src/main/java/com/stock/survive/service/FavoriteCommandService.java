package com.stock.survive.service;

public interface FavoriteCommandService {
   // 토글로 관심종목 등록&취소
    boolean toggleFavorite(Integer userId, /*Short or Integer*/ Integer itemNo);
}
