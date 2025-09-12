package com.stock.survive.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "stock_data")
public class Kospi200DataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false)
    private String ticker;

    @Column(length = 100, nullable = false)
    private String companyName;

    @Column(nullable = false)
    private LocalDate date;

    @Column
    private Integer openPrice;

    @Column
    private Integer highPrice;

    @Column
    private Integer lowPrice;

    @Column
    private Integer closePrice;

    @Column
    private Long volume;

    @Column
    private Long marketCap;

}
