#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BIG KINDS 뉴스 크롤링 자동화 - Docker 전용 실행 스크립트
사용자 입력 없이 자동으로 실행됩니다.
"""

import os
from datetime import datetime, timedelta
from bigkinds_automation import BigKindsAutomation
from config import LOGIN_EMAIL, LOGIN_PASSWORD, DEFAULT_SEARCH_PERIOD_DAYS

def main():
    """메인 실행 함수 - Docker 환경용"""
    try:
        print("=" * 60)
        print("🐳 BIG KINDS 뉴스 크롤링 자동화 (Docker 모드)")
        print("=" * 60)
        
        # 환경 변수에서 검색 기간 가져오기
        days = int(os.environ.get('SEARCH_PERIOD_DAYS', DEFAULT_SEARCH_PERIOD_DAYS))
        
        # 검색 기간 설정
        end_date = datetime.now() - timedelta(days=1)
        start_date = end_date - timedelta(days=days)
        
        start_date_str = start_date.strftime('%Y-%m-%d')
        end_date_str = end_date.strftime('%Y-%m-%d')
        
        print(f"🔍 검색 기간: {start_date_str} ~ {end_date_str}")
        print(f"📅 최근 {days}일간의 뉴스를 검색합니다")
        print("🚀 자동화를 시작합니다...")
        print()
        
        # 자동화 실행
        automation = BigKindsAutomation(LOGIN_EMAIL, LOGIN_PASSWORD)

        # 순차 실행 (run_automation 메서드 사용)
        success = automation.run_automation(start_date_str, end_date_str)

        if success:
            print("\n" + "=" * 60)
            print("✅ 자동화가 성공적으로 완료되었습니다!")
            print("📁 다운로드된 엑셀 파일을 확인해주세요:")
            print("   - Docker 볼륨: ./downloads/")
            print("   - 컨테이너 내부: /app/downloads/")
            print("=" * 60)
        else:
            print("\n" + "=" * 60)
            print("❌ 자동화 실행 중 오류가 발생했습니다.")
            print("📋 로그 파일을 확인해주세요:")
            print("   - Docker 볼륨: ./logs/")
            print("   - 컨테이너 내부: /app/logs/")
            print("=" * 60)
            
    except Exception as e:
        print(f"\n❌ 예상치 못한 오류가 발생했습니다: {e}")
        print("로그 파일을 확인해주세요.")
        return False
    
    return True

if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
