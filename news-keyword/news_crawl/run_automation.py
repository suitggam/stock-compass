#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BIG KINDS 뉴스 크롤링 자동화 실행 스크립트
"""

import sys
import os
from datetime import datetime, timedelta
from bigkinds_automation import BigKindsAutomation
from config import LOGIN_EMAIL, LOGIN_PASSWORD, DEFAULT_SEARCH_PERIOD_DAYS

def is_docker_environment():
    """Docker 환경인지 확인"""
    return os.environ.get('DOCKER_ENV') == 'true' or os.path.exists('/.dockerenv')

def get_user_input():
    """사용자로부터 검색 기간 입력 받기"""
    print("=" * 50)
    print("BIG KINDS 뉴스 크롤링 자동화")
    print("=" * 50)
    
    # Docker 환경 확인
    if is_docker_environment():
        print("🐳 Docker 환경에서 실행 중입니다.")
        print("자동 모드로 기본 설정을 사용합니다.")
        return get_default_dates()
    
    # 기본 기간 제안
    end_date = datetime.now()
    start_date = end_date - timedelta(days=DEFAULT_SEARCH_PERIOD_DAYS)
    
    print(f"기본 검색 기간: {start_date.strftime('%Y-%m-%d')} ~ {end_date.strftime('%Y-%m-%d')}")
    print(f"(최근 {DEFAULT_SEARCH_PERIOD_DAYS}일)")
    
    # 사용자 입력 받기
    while True:
        try:
            choice = input("\n기본 기간을 사용하시겠습니까? (y/n): ").lower().strip()
            
            if choice in ['y', 'yes', '']:
                return start_date.strftime('%Y-%m-%d'), end_date.strftime('%Y-%m-%d')
            elif choice in ['n', 'no']:
                # 사용자 지정 기간 입력
                print("\n검색 기간을 입력해주세요.")
                start_str = input(f"시작일 (YYYY-MM-DD, 기본값: {start_date.strftime('%Y-%m-%d')}): ").strip()
                end_str = input(f"종료일 (YYYY-MM-DD, 기본값: {end_date.strftime('%Y-%m-%d')}): ").strip()
                
                # 기본값 처리
                if not start_str:
                    start_str = start_date.strftime('%Y-%m-%d')
                if not end_str:
                    end_str = end_date.strftime('%Y-%m-%d')
                
                # 날짜 형식 검증
                start_date = datetime.strptime(start_str, '%Y-%m-%d')
                end_date = datetime.strptime(end_str, '%Y-%m-%d')
                
                if start_date > end_date:
                    print("오류: 시작일이 종료일보다 늦을 수 없습니다.")
                    continue
                
                return start_str, end_str
            else:
                print("y 또는 n을 입력해주세요.")
                
        except ValueError:
            print("오류: 올바른 날짜 형식(YYYY-MM-DD)을 입력해주세요.")
        except KeyboardInterrupt:
            print("\n\n프로그램이 중단되었습니다.")
            sys.exit(0)
        except EOFError:
            print("\n입력 스트림 오류가 발생했습니다. 기본값을 사용합니다.")
            return get_default_dates()

def get_default_dates():
    """기본 날짜 반환"""
    end_date = datetime.now()
    start_date = end_date - timedelta(days=DEFAULT_SEARCH_PERIOD_DAYS)
    return start_date.strftime('%Y-%m-%d'), end_date.strftime('%Y-%m-%d')

def main():
    """메인 실행 함수"""
    try:
        # 사용자 입력 받기
        start_date, end_date = get_user_input()
        
        print(f"\n검색 기간: {start_date} ~ {end_date}")
        print("자동화를 시작합니다...")
        
        # 자동화 실행
        automation = BigKindsAutomation(LOGIN_EMAIL, LOGIN_PASSWORD)
        
        success = automation.run_automation(start_date, end_date)
        
        if success:
            print("\n✅ 자동화가 성공적으로 완료되었습니다!")
            print("다운로드된 엑셀 파일을 확인해주세요.")
        else:
            print("\n❌ 자동화 실행 중 오류가 발생했습니다.")
            print("로그 파일(bigkinds_automation.log)을 확인해주세요.")
            
    except KeyboardInterrupt:
        print("\n\n프로그램이 사용자에 의해 중단되었습니다.")
    except EOFError:
        print("\n❌ 입력 스트림 오류가 발생했습니다.")
        print("Docker 환경에서는 자동 모드로 실행됩니다.")
        print("환경 변수 DOCKER_ENV=true를 설정해주세요.")
    except Exception as e:
        print(f"\n❌ 예상치 못한 오류가 발생했습니다: {e}")
        print("로그 파일을 확인해주세요.")
    finally:
        print("\n프로그램을 종료합니다.")

if __name__ == "__main__":
    main()
