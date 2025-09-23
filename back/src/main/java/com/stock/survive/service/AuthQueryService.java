package com.stock.survive.service;

import com.stock.survive.dto.UserSummaryDto;

public interface AuthQueryService {
    UserSummaryDto me(Integer userId);
}
