package com.stock.survive.security.refresh;

import java.time.Duration;

public interface RefreshTokenStore {
    String issue(Integer userNo, Duration ttl);
    Integer verify(String raw);
    void revoke(String raw);
}