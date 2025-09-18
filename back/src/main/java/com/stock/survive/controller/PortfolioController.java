package com.stock.survive.controller;

import com.stock.survive.dto.PortfolioSummaryDto;
import com.stock.survive.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/holdings")
    public ResponseEntity<PortfolioSummaryDto> getHoldings(Authentication auth) {
        Integer uid = extractUid(auth);
        PortfolioSummaryDto body = portfolioService.getHoldingsSummary(uid);
        return ResponseEntity.ok(body);
    }

    private Integer extractUid(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        Object p = auth.getPrincipal();
        if (p instanceof Integer i) return i;
        if (p instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(p)); } catch (Exception ignored) {}
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_PRINCIPAL");
    }
}

