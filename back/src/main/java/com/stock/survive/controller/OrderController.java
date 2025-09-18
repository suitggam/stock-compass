package com.stock.survive.controller;

import com.stock.survive.dto.OrderRequest;
import com.stock.survive.dto.OrderResponse;
import com.stock.survive.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("주문 생성 요청: {}", request);
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tradeNo}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long tradeNo,
            @RequestParam Integer userNo) {
        log.info("주문 취소 요청: tradeNo={}, userNo={}", tradeNo, userNo);
        OrderResponse response = orderService.cancelOrder(tradeNo, userNo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userNo}")
    public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable Integer userNo) {
        log.info("사용자 주문 조회: userNo={}", userNo);
        List<OrderResponse> orders = orderService.getUserOrders(userNo);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<OrderResponse>> getPendingOrders() {
        log.info("대기 주문 조회");
        List<OrderResponse> orders = orderService.getPendingOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{tradeNo}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long tradeNo) {
        log.info("주문 조회: tradeNo={}", tradeNo);
        OrderResponse order = orderService.getOrder(tradeNo);
        return ResponseEntity.ok(order);
    }
}
