# stock_data_system.py - 올인원 주식 데이터 시스템 (Docker 버전)
import time
import pandas as pd
from pykrx import stock
from datetime import datetime, timedelta
import pymysql
from sqlalchemy import create_engine, text
import warnings
import logging
import sys
import argparse
import os

warnings.filterwarnings('ignore')

# MySQL 연결 설정 - 환경변수 사용
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', '1234'),
    'database': os.getenv('DB_NAME', 'kospi200'),
    'charset': 'utf8mb4'
}

class StockDataSystem:
    def __init__(self):
        self.setup_logging()
        self.engine = None
        
    def setup_logging(self):
        """로깅 설정"""
        # 로그 디렉토리 확인/생성
        log_dir = "/app/logs"
        os.makedirs(log_dir, exist_ok=True)
        
        log_file = f"{log_dir}/stock_system_{datetime.now().strftime('%Y%m')}.log"
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file, encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        
        # DB 설정 로그 (비밀번호 제외)
        logging.info(f"DB 연결 설정: {DB_CONFIG['user']}@{DB_CONFIG['host']}/{DB_CONFIG['database']}")
        
    def create_connection(self, max_retries=5):
        """MySQL 연결 생성 (재시도 로직 포함)"""
        for attempt in range(max_retries):
            try:
                self.engine = create_engine(
                    f"mysql+pymysql://{DB_CONFIG['user']}:{DB_CONFIG['password']}@{DB_CONFIG['host']}/{DB_CONFIG['database']}?charset={DB_CONFIG['charset']}"
                )
                
                # 연결 테스트
                with self.engine.connect() as conn:
                    conn.execute(text("SELECT 1"))
                
                logging.info("✅ MySQL 연결 성공")
                return True
            except Exception as e:
                if attempt < max_retries - 1:
                    wait_time = (attempt + 1) * 5
                    logging.warning(f"MySQL 연결 실패 (시도 {attempt + 1}/{max_retries}). {wait_time}초 후 재시도: {e}")
                    time.sleep(wait_time)
                else:
                    logging.error(f"❌ MySQL 연결 최종 실패: {e}")
                    return False
    
    def create_table(self):
        """주식 데이터 테이블 생성"""
        try:
            with self.engine.connect() as conn:
                create_table_query = """
                CREATE TABLE IF NOT EXISTS stock_data (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    ticker VARCHAR(10) NOT NULL,
                    company_name VARCHAR(100) NOT NULL,
                    date DATE NOT NULL,
                    open_price INT,
                    high_price INT,
                    low_price INT,
                    close_price INT,
                    volume BIGINT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_ticker (ticker),
                    INDEX idx_date (date),
                    INDEX idx_ticker_date (ticker, date),
                    UNIQUE KEY unique_ticker_date (ticker, date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
                
                conn.execute(text(create_table_query))
                conn.commit()
                logging.info("✅ stock_data 테이블 확인/생성 완료")
                return True
                
        except Exception as e:
            logging.error(f"❌ 테이블 생성 실패: {e}")
            return False
    
    def is_trading_day(self, date):
        """거래일 확인 (주말만 체크, 공휴일은 API 응답으로 자연스럽게 처리)"""
        return date.weekday() < 5
    
    def get_last_trading_day(self):
        """마지막 거래일 계산"""
        today = datetime.now()
        check_date = today - timedelta(days=1)
        
        for _ in range(10):
            if self.is_trading_day(check_date):
                return check_date
            check_date -= timedelta(days=1)
        
        return today - timedelta(days=1)
    
    def check_data_exists(self, date_str):
        """데이터 존재 확인"""
        try:
            with self.engine.connect() as conn:
                result = conn.execute(
                    text("SELECT COUNT(*) FROM stock_data WHERE date = %s"),
                    (date_str,)
                )
                return result.fetchone()[0] > 0
        except:
            return False
    
    def collect_historical_data(self, years=5):
        """과거 데이터 수집 (5년치)"""
        logging.info(f"🏗️ {years}년치 히스토리컬 데이터 수집 시작")
        
        # 기존 테이블 삭제하고 새로 시작
        try:
            with self.engine.connect() as conn:
                conn.execute(text("DROP TABLE IF EXISTS stock_data"))
                conn.commit()
            self.create_table()
        except Exception as e:
            logging.error(f"테이블 초기화 실패: {e}")
            return False
        
        # 날짜 설정
        end_date = datetime.now()
        start_date = end_date - timedelta(days=years * 365)
        start_date_str = start_date.strftime("%Y%m%d")
        end_date_str = end_date.strftime("%Y%m%d")
        
        logging.info(f"수집 기간: {start_date_str} ~ {end_date_str}")
        
        # 코스피 200 종목
        try:
            tickers = stock.get_index_portfolio_deposit_file("1028")
            logging.info(f"총 {len(tickers)}개 종목")
        except Exception as e:
            logging.error(f"종목 리스트 가져오기 실패: {e}")
            return False
        
        success_count = 0
        fail_count = 0
        
        for i, ticker in enumerate(tickers):
            try:
                df = stock.get_market_ohlcv(start_date_str, end_date_str, ticker)
                
                if df.empty:
                    fail_count += 1
                    continue
                
                company_name = stock.get_market_ticker_name(ticker)
                
                if self.save_stock_data(ticker, company_name, df):
                    success_count += 1
                    logging.info(f"✅ [{i+1:3d}/{len(tickers)}] {ticker} ({company_name}) - {len(df)}건")
                else:
                    fail_count += 1
                
                time.sleep(1)
                
            except Exception as e:
                fail_count += 1
                logging.error(f"❌ [{i+1:3d}/{len(tickers)}] {ticker} - {str(e)}")
                continue
            
            if (i + 1) % 20 == 0:
                progress = (i + 1) / len(tickers) * 100
                logging.info(f"📊 진행률: {progress:.1f}% (성공: {success_count}, 실패: {fail_count})")
        
        logging.info(f"🎉 히스토리컬 데이터 수집 완료! 성공: {success_count}, 실패: {fail_count}")
        return True
    
    def update_daily_data(self):
        """일일 데이터 업데이트"""
        logging.info("📅 일일 데이터 업데이트 시작")
        
        # 대상 날짜
        last_trading_day = self.get_last_trading_day()
        target_date = last_trading_day.strftime("%Y%m%d")
        target_date_sql = last_trading_day.strftime("%Y-%m-%d")
        
        logging.info(f"수집 대상: {target_date} ({last_trading_day.strftime('%A')})")
        
        # 중복 확인
        if self.check_data_exists(target_date_sql):
            logging.info(f"{target_date} 데이터가 이미 존재합니다.")
            return True
        
        # 코스피 200 종목
        try:
            tickers = stock.get_index_portfolio_deposit_file("1028")
            logging.info(f"총 {len(tickers)}개 종목 처리")
        except Exception as e:
            logging.error(f"종목 리스트 가져오기 실패: {e}")
            return False
        
        success_count = 0
        fail_count = 0
        
        for i, ticker in enumerate(tickers):
            try:
                df = stock.get_market_ohlcv(target_date, target_date, ticker)
                
                if df.empty:
                    fail_count += 1
                    continue
                
                company_name = stock.get_market_ticker_name(ticker)
                
                if self.save_stock_data(ticker, company_name, df):
                    success_count += 1
                else:
                    fail_count += 1
                
                time.sleep(0.3)
                
            except Exception as e:
                fail_count += 1
                logging.warning(f"{ticker} 실패: {str(e)}")
                continue
            
            if (i + 1) % 50 == 0:
                progress = (i + 1) / len(tickers) * 100
                logging.info(f"진행률: {progress:.1f}% ({i+1}/{len(tickers)})")
        
        # 결과 확인
        saved_count = 0
        try:
            with self.engine.connect() as conn:
                result = conn.execute(
                    text("SELECT COUNT(*) FROM stock_data WHERE date = %s"),
                    (target_date_sql,)
                )
                saved_count = result.fetchone()[0]
        except:
            pass
        
        logging.info(f"✅ 일일 업데이트 완료! 성공: {success_count}, 실패: {fail_count}, DB저장: {saved_count}건")
        return True
    
    def save_stock_data(self, ticker, company_name, df):
        """주식 데이터 저장"""
        try:
            df_copy = df.copy()
            df_copy['ticker'] = ticker
            df_copy['company_name'] = company_name
            df_copy.reset_index(inplace=True)
            
            df_copy.rename(columns={
                '날짜': 'date',
                '시가': 'open_price',
                '고가': 'high_price',
                '저가': 'low_price',
                '종가': 'close_price',
                '거래량': 'volume'
            }, inplace=True)
            
            columns_order = ['ticker', 'company_name', 'date', 'open_price', 'high_price', 'low_price', 'close_price', 'volume']
            df_copy = df_copy[columns_order]
            
            df_copy.to_sql(
                name='stock_data',
                con=self.engine,
                if_exists='append',
                index=False,
                method='multi'
            )
            
            return True
            
        except Exception as e:
            logging.error(f"데이터 저장 실패 {ticker}: {e}")
            return False
    
    def show_status(self):
        """현재 상태 표시"""
        if not self.engine:
            print("❌ 데이터베이스 연결이 필요합니다.")
            return
        
        try:
            with self.engine.connect() as conn:
                # 총 레코드 수
                result = conn.execute(text("SELECT COUNT(*) FROM stock_data"))
                total_records = result.fetchone()[0]
                
                # 종목 수
                result = conn.execute(text("SELECT COUNT(DISTINCT ticker) FROM stock_data"))
                unique_tickers = result.fetchone()[0]
                
                # 날짜 범위
                result = conn.execute(text("SELECT MIN(date), MAX(date) FROM stock_data"))
                date_range = result.fetchone()
                
                # 최근 5일 데이터
                result = conn.execute(text("""
                    SELECT date, COUNT(*) as count 
                    FROM stock_data 
                    GROUP BY date 
                    ORDER BY date DESC 
                    LIMIT 5
                """))
                recent_data = result.fetchall()
                
                print("📊 데이터베이스 현황")
                print("=" * 50)
                print(f"총 레코드: {total_records:,}건")
                print(f"종목 수: {unique_tickers}개")
                print(f"기간: {date_range[0]} ~ {date_range[1]}")
                print(f"\n📅 최근 5일 데이터:")
                for date, count in recent_data:
                    print(f"  {date}: {count}건")
                
                # 삼성전자 최근 데이터
                result = conn.execute(text("""
                    SELECT date, close_price, volume 
                    FROM stock_data 
                    WHERE ticker = '005930' 
                    ORDER BY date DESC 
                    LIMIT 3
                """))
                samsung_data = result.fetchall()
                
                if samsung_data:
                    print(f"\n📈 삼성전자 최근 3일:")
                    for date, price, volume in samsung_data:
                        print(f"  {date}: {price:,}원 ({volume:,}주)")
                
        except Exception as e:
            print(f"❌ 상태 확인 실패: {e}")
    
    def run_scheduler(self):
        """24시간 스케줄러 실행"""
        try:
            import schedule
        except ImportError:
            logging.error("❌ schedule 패키지가 필요합니다: pip install schedule")
            return
        
        logging.info("🚀 24시간 스케줄러 시작 (매일 오후 7시 실행)")
        logging.info("중단하려면 Ctrl+C를 누르세요")
        
        # 매일 오후 7시에 실행
        schedule.every().day.at("19:00").do(self.update_daily_data)
        
        # 시작 시 한 번 실행
        logging.info("시작 시 테스트 업데이트...")
        self.update_daily_data()
        
        while True:
            try:
                schedule.run_pending()
                time.sleep(60)
            except KeyboardInterrupt:
                logging.info("스케줄러가 중단되었습니다.")
                break
            except Exception as e:
                logging.error(f"스케줄러 오류: {e}")
                time.sleep(300)

def main():
    parser = argparse.ArgumentParser(description='통합 주식 데이터 시스템')
    parser.add_argument('command', nargs='?', choices=['init', 'update', 'status', 'scheduler'], 
                       default='update', help='실행할 명령')
    parser.add_argument('--years', type=int, default=5, help='초기 데이터 수집 연수 (기본: 5년)')
    
    args = parser.parse_args()
    
    system = StockDataSystem()
    
    # MySQL 연결
    if not system.create_connection():
        print("❌ MySQL 연결 실패")
        sys.exit(1)
    
    # 테이블 생성
    if not system.create_table():
        print("❌ 테이블 생성 실패")
        sys.exit(1)
    
    # 명령 실행
    if args.command == 'init':
        print(f"🏗️ {args.years}년치 초기 데이터 수집 시작...")
        success = system.collect_historical_data(args.years)
        sys.exit(0 if success else 1)
        
    elif args.command == 'update':
        print("📅 일일 업데이트 실행...")
        success = system.update_daily_data()
        sys.exit(0 if success else 1)
        
    elif args.command == 'status':
        print("📊 현재 상태 확인...")
        system.show_status()
        
    elif args.command == 'scheduler':
        print("🔄 24시간 스케줄러 시작...")
        system.run_scheduler()

if __name__ == "__main__":
    # 인자 없이 실행하면 기본적으로 일일 업데이트
    main()

# 사용 예제:
# python stock_data_system.py init         # 5년치 초기 데이터 수집
# python stock_data_system.py update       # 일일 업데이트 (기본값)
# python stock_data_system.py status       # 현재 상태 확인
# python stock_data_system.py scheduler    # 24시간 스케줄러 실행
# python stock_data_system.py init --years 3  # 3년치 데이터 수집