package com.stock.survive.serviceImpl;

import com.stock.survive.dto.PageRequestDto;
import com.stock.survive.dto.PageResponseDto;
import com.stock.survive.dto.StockEndDayDto;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.service.StockItemsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StockItemsServiceImpl implements StockItemsService {

    private final StockItemsRepository stockItemsRepository;

    @Override
    public PageResponseDto<StockEndDayDto> getEndDayData(PageRequestDto pageRequestDto, LocalDate targetDate) {
        // 1️⃣ 페이지 설정
        int pageSize = 21;
        int pageNum = pageRequestDto.getPage() - 1;

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("ticker").ascending());

        // 2️⃣ targetDate가 null이면 DB에 있는 최신 날짜를 사용
        if (targetDate == null) {
            targetDate = stockItemsRepository.findMaxDate(); // Repository에서 MAX(date) 조회
        }

        // 3️⃣ Repository에서 데이터 조회
        Page<StockEndDayDto> page = stockItemsRepository.getEndOfDayData(targetDate, pageable);

        // 4️⃣ 각 DTO에 rate 계산
        page.getContent().forEach(dto -> {
            if (dto.getStartPrice() != null && dto.getStartPrice() != 0 && dto.getEndPrice() != null) {
                double rate = ((dto.getEndPrice() - dto.getStartPrice()) * 100.0 / dto.getStartPrice());
                dto.setRate(Math.round(rate * 100.0) / 100.0); // 소수점 둘째자리까지
            }
        });

        // 5️⃣ PageResponseDto로 반환
        return PageResponseDto.<StockEndDayDto>withAll()
                .dtoList(page.getContent())
                .pageRequestDto(PageRequestDto.builder()
                        .page(pageRequestDto.getPage())
                        .size(pageSize)
                        .build())
                .total(page.getTotalElements())
                .build();
    }

    @Override
    public LocalDate getLatestDataDate() {
        return stockItemsRepository.findMaxDate();
    }

}
