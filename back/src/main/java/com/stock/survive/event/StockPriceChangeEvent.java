package com.stock.survive.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StockPriceChangeEvent extends ApplicationEvent {
    
    private String ticker;
    private Integer oldPrice;
    private Integer newPrice;
    private Integer itemNo;
    
    public StockPriceChangeEvent(Object source, String ticker, Integer oldPrice, Integer newPrice, Integer itemNo) {
        super(source);
        this.ticker = ticker;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.itemNo = itemNo;
    }
}
