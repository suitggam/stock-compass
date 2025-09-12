#!/usr/bin/env python3
"""
FastAPI 클라이언트 테스트 스크립트
"""

import requests
import json

# API 서버 URL
BASE_URL = "http://localhost:8000"

def test_health_check():
    """헬스체크 테스트"""
    try:
        response = requests.get(f"{BASE_URL}/health")
        print("=== 헬스체크 테스트 ===")
        print(f"상태 코드: {response.status_code}")
        print(f"응답: {response.json()}")
        return response.status_code == 200
    except requests.exceptions.RequestException as e:
        print(f"헬스체크 실패: {e}")
        return False

def test_keyword_extraction():
    """키워드 추출 테스트"""
    try:
        # 테스트 요청 데이터 (실제 CSV에 있는 기관명 사용)
        test_data = {
            "company_name": "영주선비도서관",
            "start_date": "20200901",
            "end_date": "20200903",
            "top_keywords": 10
        }
        
        print("\n=== 키워드 추출 테스트 ===")
        print(f"요청 데이터: {json.dumps(test_data, ensure_ascii=False, indent=2)}")
        
        response = requests.post(
            f"{BASE_URL}/extract-keywords",
            json=test_data,
            headers={"Content-Type": "application/json"}
        )
        
        print(f"상태 코드: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print("응답 데이터:")
            print(json.dumps(result, ensure_ascii=False, indent=2))
            return True
        else:
            print(f"오류 응답: {response.text}")
            return False
            
    except requests.exceptions.RequestException as e:
        print(f"키워드 추출 테스트 실패: {e}")
        return False

def main():
    """메인 테스트 함수"""
    print("FastAPI 서버 테스트를 시작합니다...")
    print("서버가 http://localhost:8000에서 실행되고 있는지 확인하세요.")
    
    # 헬스체크 테스트
    if not test_health_check():
        print("❌ 헬스체크 실패. 서버가 실행되고 있는지 확인하세요.")
        return
    
    print("✅ 헬스체크 성공")
    
    # 키워드 추출 테스트
    if test_keyword_extraction():
        print("✅ 키워드 추출 테스트 성공")
    else:
        print("❌ 키워드 추출 테스트 실패")

if __name__ == "__main__":
    main()
