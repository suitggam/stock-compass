#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BIG KINDS 로그인 테스트 스크립트
"""

import os
import time
from bigkinds_automation import BigKindsAutomation
from selenium.webdriver.common.by import By

def test_login():
    """로그인 기능만 테스트"""
    try:
        print("=" * 50)
        print("BIG KINDS 로그인 테스트")
        print("=" * 50)
        
        # 로그인 정보
        email = "jack0810@kookmin.ac.kr"
        password = "0810jack!"
        
        print(f"이메일: {email}")
        print("로그인 테스트를 시작합니다...")
        
        # 자동화 객체 생성
        automation = BigKindsAutomation(email, password)
        
        # 드라이버 설정
        if not automation.setup_driver():
            print("❌ 드라이버 설정 실패")
            return False
        
        # BIG KINDS 접속
        print("🌐 BIG KINDS 웹사이트에 접속 중...")
        automation.driver.get("https://www.bigkinds.or.kr/")
        time.sleep(3)
        
        # 로그인 테스트
        print("🔐 로그인 시도 중...")
        if automation.login():
            print("✅ 로그인 성공!")
            
            # 로그인 후 상태 확인
            print("📋 현재 페이지 정보:")
            print(f"   - URL: {automation.driver.current_url}")
            print(f"   - 제목: {automation.driver.title}")
            
            # 로그인 후 나타나는 요소들 확인
            success_indicators = [
                "//a[contains(text(), '로그아웃')]",
                "//a[contains(text(), '마이페이지')]",
                "//span[contains(text(), '님')]",
                "//div[contains(@class, 'user-info')]",
                "//*[contains(@class, 'logout')]"
            ]
            
            print("🔍 로그인 성공 지표 확인:")
            for indicator in success_indicators:
                try:
                    element = automation.driver.find_element(By.XPATH, indicator)
                    print(f"   ✅ {indicator}: {element.text}")
                except:
                    print(f"   ❌ {indicator}: 찾을 수 없음")
            
            return True
        else:
            print("❌ 로그인 실패")
            return False
            
    except Exception as e:
        print(f"❌ 테스트 중 오류 발생: {e}")
        return False
        
    finally:
        if 'automation' in locals() and automation.driver:
            print("🔄 브라우저 종료 중...")
            automation.close()

if __name__ == "__main__":
    success = test_login()
    if success:
        print("\n🎉 로그인 테스트 성공!")
    else:
        print("\n💥 로그인 테스트 실패!")
