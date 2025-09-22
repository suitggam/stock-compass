package com.stock.survive.serviceImpl;

import com.stock.survive.dto.GameLatestResultDto;
import com.stock.survive.entity.GameResult;
import com.stock.survive.repository.GameResultRepository;
import com.stock.survive.service.GameResultService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GameResultServiceImpl implements GameResultService {

    private final GameResultRepository gameResultRepository;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GameLatestResultDto getLatestByUserNo(Integer userNo) {
        GameResult gr = gameResultRepository
                .findTopByUserNoOrderByGameTimeDesc(userNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GAME_RESULT_NOT_FOUND"));
        return GameLatestResultDto.from(gr);
    }
}

