package com.stock.survive.service;

import com.stock.survive.dto.UserSummaryDto;

public interface UserCommandService {
    UserSummaryDto changeNickname(Long userId, String nickname);
    void deleteMe(Long userId);
}
