package com.stock.survive.serviceImpl;

import com.stock.survive.dto.GameResultDto;
import com.stock.survive.dto.tendency.TendencyGameFinishRequest;
import com.stock.survive.dto.tendency.TendencyGameResponse;
import com.stock.survive.entity.GameResultEntity;
import com.stock.survive.repository.GameResultRepository;
import com.stock.survive.service.GameResultService;
import com.stock.survive.service.TendencyGameService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GameResultServiceImpl implements GameResultService {
    
    private final GameResultRepository gameResultRepository;
    private final TendencyGameService tendencyGameService;
    
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GameResultDto getLatestByUserNo(Long userNo) {
        GameResultEntity gr = gameResultRepository
                .findTopByUserNoOrderByCreatedAtDesc(userNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GAME_RESULT_NOT_FOUND"));
        
        return GameResultDto.from(gr);
    }
    
    @Override
    @Transactional
    public TendencyGameResponse finish(Long userId, TendencyGameFinishRequest request) {
        TendencyGameResponse response = tendencyGameService.finish(userId, request);
        
        GameResultEntity gameResultEntity = GameResultEntity.builder()
                .userNo(userId)
                .tendencyI(10) // 예시로 임시값 사용
                .tendencyE(10)
                .tendencyS(10)
                .tendencyN(10)
                .tendencyF(10)
                .tendencyT(10)
                .tendencyJ(10)
                .tendencyP(10)
                .build();
        
        gameResultRepository.save(gameResultEntity);
        
        return response;
    }
}