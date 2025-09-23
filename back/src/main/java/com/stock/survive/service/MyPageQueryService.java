package com.stock.survive.service;

import com.stock.survive.dto.MyPageDto;
import java.util.Set;

public interface MyPageQueryService {
    MyPageDto getMyPage(Integer userId);
    Set<Integer> getFavoriteIdSet(Integer userId);
}