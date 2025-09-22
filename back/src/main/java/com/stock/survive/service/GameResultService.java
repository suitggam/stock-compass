package com.stock.survive.service;

import com.stock.survive.dto.GameLatestResultDto;

public interface GameResultService {
    GameLatestResultDto getLatestByUserNo(Integer userNo);
}

