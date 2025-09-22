package com.stock.survive.service;

import com.stock.survive.dto.tendency.TendencyGameFinishRequest;
import com.stock.survive.dto.tendency.TendencyGameOrderRequest;
import com.stock.survive.dto.tendency.TendencyGameResultResponse;
import com.stock.survive.dto.tendency.TendencyGameStartRequest;
import com.stock.survive.dto.tendency.TendencyGameStateResponse;

public interface TendencyGameService {

    TendencyGameStateResponse start(Integer userId, TendencyGameStartRequest request);

    TendencyGameStateResponse getState(Integer userId, Long sessionId);

    TendencyGameStateResponse placeOrder(Integer userId, Long sessionId, TendencyGameOrderRequest request);

    TendencyGameStateResponse proceedNextWeek(Integer userId, Long sessionId);

    TendencyGameResultResponse finish(Integer userId, TendencyGameFinishRequest request);
}
