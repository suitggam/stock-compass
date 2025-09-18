package com.stock.survive.service;

import com.stock.survive.dto.OrderRequest;
import com.stock.survive.dto.OrderResponse;
import com.stock.survive.entity.TradeHistory;

import java.util.List;

public interface OrderService {
    
    OrderResponse createOrder(OrderRequest request);
    
    OrderResponse cancelOrder(Long tradeNo, Integer userNo);
    
    List<OrderResponse> getUserOrders(Integer userNo);
    
    List<OrderResponse> getPendingOrders();
    
    OrderResponse getOrder(Long tradeNo);
    
    void processPendingOrders();
    
    void expireOrders();
}
