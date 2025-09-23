package com.stock.survive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractKeywordsDto {
    private String companyName;
    private String startDate;
    private String endDate;
    private Integer topKeywords;
    private boolean useAiFilter;
    private Map<String, Integer> keywords;
    private List<TopNewsArticle> topNewsArticles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopNewsArticle {
        private String title;
        private String date;
        private String url;
        private int matchedKeywordsCount;
        private List<String> matchedKeywords;
    }
}
