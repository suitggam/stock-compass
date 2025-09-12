#!/usr/bin/env python3
"""
종합 테스트 스크립트 - 다양한 기업과 설정으로 API 테스트
"""

import requests
import json
import time

# API 서버 URL
BASE_URL = "http://localhost:8000"

def test_api_with_company(company_name, top_keywords=10):
    """특정 기업으로 API 테스트"""
    print(f"\n🔍 【{company_name}】 키워드 추출 테스트")
    print("=" * 50)
    
    test_data = {
        "company_name": company_name,
        "start_date": "20200901",
        "end_date": "20200903",
        "top_keywords": top_keywords
    }
    
    start_time = time.time()
    
    try:
        response = requests.post(
            f"{BASE_URL}/extract-keywords",
            json=test_data,
            headers={"Content-Type": "application/json"}
        )
        
        end_time = time.time()
        process_time = end_time - start_time
        
        if response.status_code == 200:
            result = response.json()
            
            # PySpark 사용 여부 확인
            is_pyspark = "PySpark로" in result["message"]
            engine = "🚀 PySpark" if is_pyspark else "🐼 pandas"
            
            print(f"✅ 상태: 성공 ({engine})")
            print(f"⏱️  처리 시간: {process_time:.2f}초")
            print(f"📰 뉴스 개수: {result['total_news_count']}개")
            print(f"🔤 키워드 개수: {len(result['keywords'])}개")
            print(f"💬 메시지: {result['message']}")
            
            if result['keywords']:
                print(f"🏆 상위 5개 키워드:")
                for i, (keyword, freq) in enumerate(list(result['keywords'].items())[:5], 1):
                    print(f"   {i}. {keyword}: {freq}회")
            
            return True
            
        else:
            print(f"❌ 오류 (상태 코드: {response.status_code})")
            print(f"   응답: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ 네트워크 오류: {e}")
        return False

def main():
    """메인 테스트 함수"""
    print("🎯 PySpark 키워드 추출 API 종합 테스트")
    print("=" * 60)
    
    # 테스트할 기업들
    test_companies = [
        ("삼성전자", 15),
        ("SK하이닉스", 20),
        ("LG전자", 10),
        ("KB국민은행", 10),
        ("하나은행", 15),
    ]
    
    success_count = 0
    total_count = len(test_companies)
    
    for company, top_k in test_companies:
        if test_api_with_company(company, top_k):
            success_count += 1
        time.sleep(1)  # API 부하 방지
    
    print(f"\n📊 테스트 결과 요약")
    print("=" * 30)
    print(f"✅ 성공: {success_count}/{total_count}")
    print(f"❌ 실패: {total_count - success_count}/{total_count}")
    print(f"📈 성공률: {(success_count/total_count)*100:.1f}%")
    
    if success_count == total_count:
        print("\n🎉 모든 테스트가 성공했습니다!")
    else:
        print(f"\n⚠️  {total_count - success_count}개 테스트에서 문제가 발생했습니다.")

if __name__ == "__main__":
    main()

