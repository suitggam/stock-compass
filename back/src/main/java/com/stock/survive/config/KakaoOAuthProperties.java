package com.stock.survive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth.kakao")
public class KakaoOAuthProperties {
    private String clientId;
    private String clientSecret;         // 선택(없으면 비워둠)
    private String redirectUri;          // 예: http://localhost:8080/users/auth/kakao/callback

    private String authorizeUri = "https://kauth.kakao.com/oauth/authorize";
    private String tokenUri     = "https://kauth.kakao.com/oauth/token";
    private String userinfoUri  = "https://kapi.kakao.com/v2/user/me";
}
