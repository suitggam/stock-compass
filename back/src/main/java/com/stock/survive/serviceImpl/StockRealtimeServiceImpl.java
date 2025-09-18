package com.stock.survive.serviceImpl;

import com.stock.survive.dto.PageRequestDto;
import com.stock.survive.dto.PageResponseDto;
import com.stock.survive.dto.StockRealtimeDto;
import com.stock.survive.entity.StockRealtime;
import com.stock.survive.event.StockPriceChangeEvent;
import com.stock.survive.repository.StockRealtimeRepository;
import com.stock.survive.service.StockRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockRealtimeServiceImpl implements StockRealtimeService {

    private final StockRealtimeRepository stockRealtimeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResponseDto<StockRealtimeDto> getList(PageRequestDto pageRequestDto) {
        Pageable pageable = PageRequest.of(
                pageRequestDto.getPage() - 1,
                pageRequestDto.getSize(),
                Sort.by("ticker").ascending()
        );

        Page<StockRealtimeDto> page = stockRealtimeRepository.findAllWithLatestInfo(pageable);

        return PageResponseDto.<StockRealtimeDto>withAll()
                .dtoList(page.getContent())
                .pageRequestDto(pageRequestDto)
                .total(page.getTotalElements())
                .build();
    }

    @Override
    @Transactional
    public void updateStockPrice(String ticker, Integer newPrice) {
        Optional<StockRealtime> stockOptional = stockRealtimeRepository.findByTicker(ticker);

        if (stockOptional.isPresent()) {
            StockRealtime stock = stockOptional.get();
            Integer oldPrice = stock.getPrice();

            // 주가 업데이트
            stock = StockRealtime.builder()
                    .realtimeNo(stock.getRealtimeNo())
                    .ticker(stock.getTicker())
                    .companyName(stock.getCompanyName())
                    .price(newPrice)
                    .rate(calculateRate(oldPrice, newPrice))
                    .itemNo(stock.getItemNo())
                    .build();

            stockRealtimeRepository.save(stock);

            // 주가 변경 이벤트 발행
            if (!newPrice.equals(oldPrice)) {
                StockPriceChangeEvent event = new StockPriceChangeEvent(this, ticker, oldPrice, newPrice, stock.getItemNo());
                eventPublisher.publishEvent(event);
                log.info("주가 변경 이벤트 발행: ticker={}, oldPrice={}, newPrice={}", ticker, oldPrice, newPrice);
            }
        } else {
            log.warn("종목을 찾을 수 없습니다: ticker={}", ticker);
        }
    }

    @Override
    public Integer getCurrentPrice(String ticker) {
        return stockRealtimeRepository.findByTicker(ticker)
                .map(StockRealtime::getPrice)
                .orElse(0);
    }

    private Double calculateRate(Integer oldPrice, Integer newPrice) {
        if (oldPrice == null || oldPrice == 0) {
            return 0.0;
        }
        return ((double) (newPrice - oldPrice) / oldPrice) * 100;
    }
}