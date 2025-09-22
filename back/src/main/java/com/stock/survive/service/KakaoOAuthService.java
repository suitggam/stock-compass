package com.stock.survive.service;

import com.stock.survive.dto.OAuthUserInfo;
import com.stock.survive.serviceImpl.KakaoOAuthServiceImpl;

public interface KakaoOAuthService {

    OAuthUserInfo exchangeAndFetchUser(String code);

}
