package com.stock.survive.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfosDto {
    private String ticker;
    private String companyName;
    private Integer endPrice;
    private LocalDate date;
}
