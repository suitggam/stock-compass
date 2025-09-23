package com.stock.survive.serviceImpl;

import com.stock.survive.dto.ExtractKeywordsDto;
import com.stock.survive.dto.ExtractKeywordsDto.TopNewsArticle;
import com.stock.survive.dto.StockInfosDto;
import com.stock.survive.entity.StockInfos;
import com.stock.survive.entity.StockItems;
import com.stock.survive.repository.StockInfosRepository;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.service.StockInfosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInfosServiceImpl implements StockInfosService {

    private final StockInfosRepository stockInfosRepository;
    private final StockItemsRepository stockItemsRepository;
    private final WebClient webClient; // WebClient 주입

    @Value("${keywords.api.url}")
    private String keywordsApiUrl; // application.properties나 application.yml에서 설정

    @Override
    public List<StockInfosDto> getStock(String ticker) {
        List<StockInfos> infos = stockInfosRepository.findRecent6YearsByTicker(ticker);
        return infos.stream()
                .map(stockInfos -> StockInfosDto.builder()
                        .ticker(stockInfos.getStockItem().getTicker())
                        .companyName(stockInfos.getStockItem().getCompanyName())
                        .endPrice(stockInfos.getEndPrice())
                        .date(stockInfos.getDate())
                        .build())
                .toList();
    }

    @Override
    public ExtractKeywordsDto getKeywords(String ticker, ExtractKeywordsDto requestDto) {
        // 1️⃣ DB에서 companyName 조회
        String companyName = stockItemsRepository.findCompanyNameByTicker(ticker)
                .map(StockItems::getCompanyName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ticker: " + ticker));

        // 2️⃣ 프론트에서 받은 값 활용 (없으면 기본값)
        String startDate = requestDto.getStartDate() != null
                ? requestDto.getStartDate().replace("-", "")
                : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = requestDto.getEndDate() != null
                ? requestDto.getEndDate().replace("-", "")
                : startDate;
        int topKeywords = requestDto.getTopKeywords() != null ? requestDto.getTopKeywords() : 10;
        boolean useAiFilter = requestDto.isUseAiFilter();

        log.info(startDate + " " + endDate);

        // 3️⃣ 외부 API 호출 payload 구성
        var payload = Map.of(
                "company_name", companyName,
                "start_date", startDate,
                "end_date", endDate,
                "top_keywords", topKeywords,
                "use_ai_filter", useAiFilter
        );

        // 4️⃣ WebClient로 외부 API 호출
        Map response = webClient.post()
                .uri(keywordsApiUrl + "/extract-keywords/ticker")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class) // 외부 API가 JSON으로 반환
                .block(); // 동기 호출

        // 5️⃣ 결과 매핑
        Map<String, Integer> keywords = (Map<String, Integer>) response.get("keywords");
        List<Map<String, Object>> topNews = (List<Map<String, Object>>) response.get("top_news_articles");
        List<TopNewsArticle> topNewsArticles = topNews.stream().map(news -> TopNewsArticle.builder()
                        .title((String) news.get("title"))
                        .date((String) news.get("date"))
                        .url((String) news.get("url"))
                        .matchedKeywordsCount((Integer) news.get("matched_keywords_count"))
                        .matchedKeywords((List<String>) news.get("matched_keywords"))
                        .build())
                .toList();

        // 6️⃣ 최종 DTO 반환
        return ExtractKeywordsDto.builder()
                .companyName(companyName)
                .startDate(startDate)
                .endDate(endDate)
                .topKeywords(topKeywords)
                .useAiFilter(useAiFilter)
                .keywords(keywords)
                .topNewsArticles(topNewsArticles)
                .build();
    }

    public Integer getLatestEndPrice(Integer itemNo) {
        return stockInfosRepository.findLatestEndPriceByItemNo(itemNo)
                .orElse(null);
    }
}
