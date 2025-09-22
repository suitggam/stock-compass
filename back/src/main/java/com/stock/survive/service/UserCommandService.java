package com.stock.survive.service;

import com.stock.survive.dto.UserSummaryDto;

public interface UserCommandService {
    UserSummaryDto changeNickname(Integer userId, String nickname);
    void deleteMe(Integer userId);
}
