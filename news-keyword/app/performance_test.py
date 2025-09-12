#!/usr/bin/env python3
"""
성능 테스트 스크립트 - 다양한 키워드 개수와 처리 시간 측정
"""

import requests
import json
import time
import statistics

BASE_URL = "http://localhost:8000"

def performance_test():
    """성능 테스트 실행"""
    print("⚡ PySpark API 성능 테스트")
    print("=" * 40)
    
    # 다양한 키워드 개수로 테스트
    test_cases = [
        ("정부", 5),
        ("정부", 10), 
        ("정부", 20),
        ("정부", 50),
    ]
    
    results = []
    
    for company, top_k in test_cases:
        print(f"\n📊 테스트: {company} (상위 {top_k}개 키워드)")
        print("-" * 30)
        
        times = []
        
        # 3번 반복 측정
        for i in range(3):
            start_time = time.time()
            
            response = requests.post(
                f"{BASE_URL}/extract-keywords",
                json={
                    "company_name": company,
                    "start_date": "20200901",
                    "end_date": "20200903",
                    "top_keywords": top_k
                }
            )
            
            end_time = time.time()
            process_time = end_time - start_time
            times.append(process_time)
            
            if response.status_code == 200:
                result = response.json()
                if i == 0:  # 첫 번째 실행 결과만 출력
                    print(f"   📰 뉴스 개수: {result['total_news_count']}")
                    print(f"   🔤 키워드 개수: {len(result['keywords'])}")
            
            print(f"   🕐 실행 {i+1}: {process_time:.2f}초")
            time.sleep(0.5)
        
        avg_time = statistics.mean(times)
        results.append((f"{company} (top {top_k})", avg_time))
        print(f"   📈 평균 시간: {avg_time:.2f}초")
    
    print(f"\n🏆 성능 테스트 결과 요약")
    print("=" * 35)
    for test_name, avg_time in results:
        print(f"   {test_name}: {avg_time:.2f}초")

if __name__ == "__main__":
    performance_test()

