package com.stock.survive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemOptionDto {
    private Integer itemNo;
    private String ticker;
    private String companyName;
}

