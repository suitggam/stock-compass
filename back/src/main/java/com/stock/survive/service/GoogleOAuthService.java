package com.stock.survive.service;

import com.stock.survive.dto.OAuthUserInfo;
import com.stock.survive.serviceImpl.GoogleOAuthServiceImpl;

public interface GoogleOAuthService {

    OAuthUserInfo exchangeAndFetchUser(String code);


}
