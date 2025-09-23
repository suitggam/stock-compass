package com.stock.survive.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stock.survive.entity.User;
import lombok.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 응답에서 숨김(선택)
public class MyPageDto {
    private Long userNo;
    private String socialEmail;
    private String nickname;
    private boolean cancel;
    private Integer totalReward;
    private Integer cash;
    private String createdAt;
    private String avatarUrl;

    //관심종목만 일단 구현 하고 나머지 두개는 나중에 처리하는걸로
    private List<FavoriteItemDto> favorites;           // 관심종목
    private PersonalityResultDto personality;          // 투자 성향 결과(미구현이면 null)
    private List<MockInvestmentDto> mockInvestHistory; // 모의투자 기록(미구현이면 null)

    /** 기본 정보만 채움 */
    public static MyPageDto ofBasic(User u, String avatarUrl) {
        return MyPageDto.builder()
                .userNo(u.getId())
                .socialEmail(u.getSocialEmail())
                .nickname(u.getNickname())
                .cancel(u.isCancel())
                .totalReward(u.getTotalReward())
                .cash(u.getCash())
                .createdAt(u.getCreatedAt() != null
                        ? u.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .avatarUrl(avatarUrl)
                .build(); // favorites, personality, mockInvestHistory는 null
    }

    /** 관심종목 리스트까지 채움 */
    public static MyPageDto ofWithFavorites(User u, String avatarUrl, List<FavoriteItemDto> favorites) {
        MyPageDto dto = ofBasic(u, avatarUrl);
        dto.setFavorites(favorites);
        return dto;
    }

    // ── 내부(또는 별도 파일) 서브 DTO들 ────────────────────────────────
    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class FavoriteItemDto {
        private Long itemId;
        private String name;
        // 필요시 현재가/등락률 등 추가
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class PersonalityResultDto {
        private String type;     // 예: "INT-R" 등
        private String summary;  // 한줄 요약
        // score breakdown 등 필요시 추가
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class MockInvestmentDto {
        private Long id;
        private String symbol;
        private Integer quantity;
        private Long price;      // 체결가
        private String tradedAt; // ISO 문자열
        // 손익, 수수료 등 필요시 추가
    }
}
