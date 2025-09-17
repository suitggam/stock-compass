#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import time
from bigkinds_automation import BigKindsAutomation

def main():
	"""Docker 환경에서 로그인 및 탭 이동 테스트"""
	print("=" * 60)
	print("🐳 BIG KINDS 로그인 및 탭 이동 테스트 (Docker 환경)")
	print("=" * 60)
	
	# 환경 변수 확인
	print("🔧 환경 변수 확인:")
	print(f"   - DOCKER_ENV: {os.environ.get('DOCKER_ENV', 'Not set')}")
	print(f"   - CHROME_BIN: {os.environ.get('CHROME_BIN', 'Not set')}")
	print(f"   - CHROMEDRIVER_PATH: {os.environ.get('CHROMEDRIVER_PATH', 'Not set')}")
	print(f"   - LOG_DIR: {os.environ.get('LOG_DIR', 'Not set')}")
	print(f"   - DOWNLOAD_DIR: {os.environ.get('DOWNLOAD_DIR', 'Not set')}")
	print()
	
	# 로그인 정보
	email = "jack0810@kookmin.ac.kr"
	password = "0810jack!"
	
	print("📧 로그인 정보:")
	print(f"   - 이메일: {email}")
	print(f"   - 비밀번호: {'*' * len(password)}")
	print()
	
	# 자동화 실행
	automation = BigKindsAutomation(email, password)
	
	try:
		print("🚀 로그인 및 탭 이동 테스트를 시작합니다...")
		
		# 1. 드라이버 설정
		print("🔧 드라이버 설정 중...")
		if not automation.setup_driver():
			print("❌ 드라이버 설정 실패")
			return
		
		print("✅ 드라이버 설정 완료")
		
		# 2. BIG KINDS 접속
		print("🌐 BIG KINDS 웹사이트에 접속 중...")
		automation.driver.get("https://www.bigkinds.or.kr/")
		time.sleep(3)
		
		print(f"   - 현재 URL: {automation.driver.current_url}")
		print(f"   - 제목: {automation.driver.title}")
		
		# 3. 로그인
		print("🔐 로그인 시도 중...")
		if not automation.login():
			print("❌ 로그인 실패")
			return
		print("✅ 로그인 성공!")
		print()
		
		# 4. 뉴스 검색 분석 탭으로 이동 (직접 URL)
		print("📊 뉴스 검색 분석 탭으로 이동 중...")
		if not automation.navigate_to_news_analysis():
			print("❌ 뉴스 검색 분석 탭 이동 실패")
			return
		print("✅ 뉴스 검색 분석 탭 이동 성공!")
		
		# 5. 기간 1일 선택
		print("⏱️ 기간 1일 선택 중...")
		if not automation.set_period_one_day():
			print("❌ 기간 1일 선택 실패")
			return
		print("✅ 기간 1일 선택 완료")
		
		# 6. 통합 분류 경제 적용
		print("🏷️ 통합 분류에서 '경제' 적용 중...")
		if not automation.select_economy_and_apply():
			print("❌ '경제' 적용 실패")
			return
		print("✅ '경제' 적용 완료")
		# 7. 분석 결과 및 시각화 → 엑셀 다운로드
		print("📥 분석 결과 및 시각화 탭 열고 엑셀 다운로드 중...")
		if not automation.open_analysis_and_download_excel():
			print("❌ 엑셀 다운로드 실패")
			return
		print("✅ 엑셀 다운로드 트리거 완료")
		
		print("🎉 테스트 완료: 로그인 → 분석 페이지 → 기간(1일) → 경제 적용 → 엑셀 다운로드")
		
	except KeyboardInterrupt:
		print("\n⏹️ 사용자에 의해 중단되었습니다.")
	except Exception as e:
		print(f"❌ 예상치 못한 오류가 발생했습니다: {e}")
	finally:
		# 브라우저 종료
		print("🔄 브라우저 종료 중...")
		automation.close()
		print()
		print("=" * 60)
		print("테스트 완료")
		print("=" * 60)

if __name__ == "__main__":
	main()
