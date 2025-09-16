package com.stock.survive.serviceImp;

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
public class StockItemsServiceImp implements StockItemsService {

    private final StockItemsRepository stockItemsRepository;

    @Override
    public PageResponseDto<StockEndDayDto> getEndDayData(PageRequestDto pageRequestDto, LocalDate targetDate) {

        // 1️⃣ 한 페이지 20개 고정
        int pageSize = 21;
        int pageNum = pageRequestDto.getPage() - 1;

        Pageable pageable = PageRequest.of(
                pageNum,
                pageSize,
                Sort.by("ticker").ascending() // ticker 기준 오름차순
        );

        // 2️⃣ Repository에서 페이지 단위로 데이터 조회
        Page<StockEndDayDto> page = stockItemsRepository.getEndOfDayData(targetDate, pageable);


        // 3️⃣ 각 DTO에 rate 계산
        page.getContent().forEach(dto -> {
            if (dto.getStartPrice() != null && dto.getStartPrice() != 0 && dto.getEndPrice() != null) {
                double rate = ((dto.getEndPrice() - dto.getStartPrice()) * 100.0 / dto.getStartPrice());
                dto.setRate(Math.round(rate * 100.0) / 100.0); // 소수점 둘째자리까지 반올림
            }
        });

        // 4️⃣ PageResponseDto로 감싸서 반환
        return PageResponseDto.<StockEndDayDto>withAll()
                .dtoList(page.getContent())
                .pageRequestDto(PageRequestDto.builder()
                        .page(pageRequestDto.getPage())
                        .size(pageSize)
                        .build())
                .total(page.getTotalElements())
                .build();
    }
}
