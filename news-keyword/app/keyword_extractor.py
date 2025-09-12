from typing import Optional, Dict, List
import os
import logging
import pandas as pd
import re
from collections import Counter
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, split, explode, count, collect_list, when, size, slice, regexp_replace, trim, length, lower

# 로깅 설정
logger = logging.getLogger(__name__)

class KeywordExtractor:
    """PySpark를 사용한 키워드 추출 클래스"""
    
    def __init__(self):
        self.spark = None
        self.csv_file_path = None
        
    def initialize_spark(self):
        """SparkSession 초기화 (Java 11과 PySpark 3.3.0 호환성 최적화)"""
        if self.spark is None:
            try:
                import os
                
                # Java 환경 변수 확인 및 설정 (Java 8 사용)
                java_home = os.environ.get('JAVA_HOME')
                if not java_home:
                    os.environ['JAVA_HOME'] = '/usr/lib/jvm/java-8-openjdk-amd64'
                    logger.info(f"JAVA_HOME 설정: {os.environ['JAVA_HOME']}")
                
                # SPARK_HOME 환경 변수 설정
                spark_home = os.environ.get('SPARK_HOME')
                if not spark_home:
                    os.environ['SPARK_HOME'] = '/usr/local/lib/python3.9/dist-packages/pyspark'
                    logger.info(f"SPARK_HOME 설정: {os.environ['SPARK_HOME']}")
                
                # PySpark Python 실행 파일 설정
                os.environ['PYSPARK_PYTHON'] = '/usr/bin/python'
                os.environ['PYSPARK_DRIVER_PYTHON'] = '/usr/bin/python'
                
                self.spark = SparkSession.builder \
                    .appName("NewsKeywordAPI") \
                    .master("local[*]") \
                    .config("spark.driver.memory", "2g") \
                    .config("spark.driver.maxResultSize", "1g") \
                    .config("spark.executor.memory", "2g") \
                    .config("spark.sql.adaptive.enabled", "true") \
                    .config("spark.sql.adaptive.coalescePartitions.enabled", "true") \
                    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
                    .config("spark.sql.adaptive.skewJoin.enabled", "true") \
                    .config("spark.sql.execution.arrow.pyspark.enabled", "false") \
                    .config("spark.sql.execution.arrow.enabled", "false") \
                    .config("spark.sql.shuffle.partitions", "200") \
                    .config("spark.default.parallelism", "4") \
                    .config("spark.driver.host", "localhost") \
                    .config("spark.driver.bindAddress", "0.0.0.0") \
                    .config("spark.ui.enabled", "false") \
                    .config("spark.ui.showConsoleProgress", "false") \
                    .getOrCreate()
                
                # 로그 레벨 설정 (너무 많은 로그 방지)
                self.spark.sparkContext.setLogLevel("WARN")
                
                # Java 버전 확인
                java_version = self.spark.sparkContext._jvm.System.getProperty("java.version")
                logger.info(f"SparkSession 초기화 성공! Java 버전: {java_version}")
                
            except Exception as e:
                logger.error(f"SparkSession 초기화 실패: {e}")
                logger.info("Java 버전 호환성 문제일 가능성이 높습니다.")
                # pandas 백업 플랜 사용을 위해 spark를 None으로 유지
                self.spark = None
                raise
    
    def find_csv_file(self, start_date: str, end_date: str) -> str:
        """
        날짜 범위에 해당하는 CSV 파일을 찾습니다.
        현재는 고정된 파일명을 사용하지만, 나중에 확장 가능합니다.
        """
        # 현재는 고정된 파일 사용
        csv_filename = "NewsResult_20200901-20200903__sheet.csv"
        csv_path = os.path.join("news", csv_filename)
        
        if os.path.exists(csv_path):
            logger.info(f"CSV 파일을 찾았습니다: {csv_path}")
            return csv_path
        else:
            raise FileNotFoundError(f"CSV 파일을 찾을 수 없습니다: {csv_path}")
    
    def extract_keywords_with_pandas(self, company_name: str, start_date: str, end_date: str, top_keywords: int) -> Dict:
        """
        pandas를 사용한 키워드 추출 (백업 방법)
        """
        logger.info("pandas를 사용하여 키워드 추출을 시도합니다.")
        
        # CSV 파일 경로 찾기
        csv_path = self.find_csv_file(start_date, end_date)
        
        # CSV 파일 읽기
        df = pd.read_csv(csv_path, encoding='utf-8')
        
        logger.info(f"CSV 파일 로드 완료. 총 {len(df)}개 행")
        logger.info(f"컬럼명: {list(df.columns)}")
        
        # 기업 필터링 (기관 컬럼에서 해당 기업이 포함된 행들을 가져옴)
        if '기관' in df.columns:
            # 기관 컬럼에 NaN이 아니고 회사명이 포함된 행 필터링
            mask = df['기관'].notna() & df['기관'].str.contains(company_name, na=False)
            filtered_df = df[mask]
            total_count = len(filtered_df)
            
            logger.info(f"'{company_name}' 관련 뉴스: {total_count}개")
            
            if total_count == 0:
                return {
                    "company_name": company_name,
                    "period": f"{start_date}-{end_date}",
                    "total_news_count": 0,
                    "keywords": {},
                    "top_keywords": [],
                    "message": f"'{company_name}'와 관련된 뉴스를 찾을 수 없습니다."
                }
            
            # 키워드 추출 (기존 키워드 컬럼 사용)
            if '키워드' in df.columns:
                all_keywords = []
                
                # 각 행의 키워드를 분리하고 정리
                for keywords_str in filtered_df['키워드'].dropna():
                    if pd.notna(keywords_str) and keywords_str.strip():
                        # 쉼표로 분리
                        keywords = keywords_str.split(',')
                        
                        # 각 키워드 정리
                        for keyword in keywords:
                            # 공백 제거
                            keyword = keyword.strip()
                            # 특수문자 제거 (한글, 영문, 숫자만 유지)
                            keyword = re.sub(r'[^가-힣a-zA-Z0-9\s]', '', keyword)
                            # 연속된 공백을 하나로
                            keyword = re.sub(r'\s+', ' ', keyword).strip()
                            
                            # 길이가 2 이상인 키워드만 추가
                            if len(keyword) >= 2:
                                all_keywords.append(keyword)
                
                # 키워드 빈도 계산
                keyword_counter = Counter(all_keywords)
                
                # 빈도순으로 정렬하여 딕셔너리 생성
                keywords_dict = dict(keyword_counter.most_common())
                
                # 상위 키워드 추출
                top_keywords_list = list(keywords_dict.keys())[:top_keywords]
                
                return {
                    "company_name": company_name,
                    "period": f"{start_date}-{end_date}",
                    "total_news_count": total_count,
                    "keywords": keywords_dict,
                    "top_keywords": top_keywords_list,
                    "message": f"pandas로 성공적으로 키워드를 추출했습니다. 총 {len(keywords_dict)}개 키워드 발견"
                }
            else:
                return {
                    "company_name": company_name,
                    "period": f"{start_date}-{end_date}",
                    "total_news_count": total_count,
                    "keywords": {},
                    "top_keywords": [],
                    "message": "키워드 컬럼을 찾을 수 없습니다."
                }
        else:
            raise ValueError("기관 컬럼을 찾을 수 없습니다.")

    def extract_keywords_from_csv(self, company_name: str, start_date: str, end_date: str, top_keywords: int) -> Dict:
        """
        CSV 파일에서 특정 기업의 키워드를 추출합니다.
        PySpark를 먼저 시도하고, 실패 시 pandas로 폴백합니다.
        """
        try:
            # SparkSession 초기화 시도
            self.initialize_spark()
            
            if self.spark is None:
                logger.warning("PySpark 초기화 실패, pandas로 폴백합니다.")
                return self.extract_keywords_with_pandas(company_name, start_date, end_date, top_keywords)
            
            # CSV 파일 경로 찾기
            csv_path = self.find_csv_file(start_date, end_date)
            
            logger.info(f"PySpark로 CSV 파일 읽기 시작: {csv_path}")
            
            # CSV 파일 읽기
            df = self.spark.read \
                .option("header", "true") \
                .option("inferSchema", "true") \
                .option("encoding", "UTF-8") \
                .option("multiline", "true") \
                .option("escape", '"') \
                .csv(csv_path)
            
            # 데이터 캐싱 (성능 향상)
            df.cache()
            
            total_rows = df.count()
            logger.info(f"CSV 파일 로드 완료. 총 {total_rows}개 행")
            logger.info(f"컬럼명: {df.columns}")
            
            # 기업 필터링 (기관 컬럼에서 해당 기업이 포함된 행들을 가져옴)
            if '기관' in df.columns:
                # 기관 컬럼이 null이 아니고 company_name이 포함된 행 필터링
                filtered_df = df.filter(
                    col('기관').isNotNull() & 
                    col('기관').contains(company_name)
                )
                
                total_count = filtered_df.count()
                logger.info(f"'{company_name}' 관련 뉴스: {total_count}개")
                
                if total_count == 0:
                    return {
                        "company_name": company_name,
                        "period": f"{start_date}-{end_date}",
                        "total_news_count": 0,
                        "keywords": {},
                        "top_keywords": [],
                        "message": f"'{company_name}'와 관련된 뉴스를 찾을 수 없습니다."
                    }
                
                # 키워드 추출 (기존 키워드 컬럼 사용)
                if '키워드' in df.columns:
                    # 키워드 컬럼에서 키워드 분리 및 정리
                    keywords_df = filtered_df.select(
                        col('기관').alias('company'),
                        explode(split(col('키워드'), ',')).alias('keyword')
                    ).filter(
                        col('keyword').isNotNull() & 
                        (col('keyword') != '')
                    )
                    
                    # 키워드 정리 (공백 제거, 특수문자 제거, 길이 필터링)
                    keywords_df = keywords_df.withColumn(
                        'keyword', 
                        trim(regexp_replace(col('keyword'), r'[^가-힣a-zA-Z0-9\s]', ''))
                    ).filter(
                        length(col('keyword')) >= 2
                    )
                    
                    # 키워드 빈도 계산
                    keyword_freq = keywords_df.groupBy('keyword') \
                        .agg(count('*').alias('frequency')) \
                        .orderBy(col('frequency').desc())
                    
                    # 상위 키워드 수집
                    keyword_list = keyword_freq.limit(top_keywords * 2).collect()  # 여유분 확보
                    
                    # Python 딕셔너리로 변환
                    keywords_dict = {row['keyword']: row['frequency'] for row in keyword_list if row['keyword'].strip()}
                    
                    # 상위 키워드 추출 (실제 개수만큼)
                    top_keywords_list = list(keywords_dict.keys())[:top_keywords]
                    
                    logger.info(f"PySpark로 키워드 추출 완료: {len(keywords_dict)}개 키워드")
                    
                    return {
                        "company_name": company_name,
                        "period": f"{start_date}-{end_date}",
                        "total_news_count": total_count,
                        "keywords": keywords_dict,
                        "top_keywords": top_keywords_list,
                        "message": f"PySpark로 성공적으로 키워드를 추출했습니다. 총 {len(keywords_dict)}개 키워드 발견"
                    }
                else:
                    return {
                        "company_name": company_name,
                        "period": f"{start_date}-{end_date}",
                        "total_news_count": total_count,
                        "keywords": {},
                        "top_keywords": [],
                        "message": "키워드 컬럼을 찾을 수 없습니다."
                    }
            else:
                raise ValueError("기관 컬럼을 찾을 수 없습니다.")
                
        except Exception as e:
            logger.error(f"PySpark로 키워드 추출 중 오류 발생: {str(e)}")
            logger.info("pandas로 폴백합니다.")
            return self.extract_keywords_with_pandas(company_name, start_date, end_date, top_keywords)
        finally:
            # 캐시 정리
            if hasattr(self, 'spark') and self.spark:
                try:
                    self.spark.catalog.clearCache()
                except:
                    pass
    
    def cleanup(self):
        """SparkSession 정리"""
        if self.spark:
            try:
                self.spark.stop()
                self.spark = None
                logger.info("SparkSession이 정리되었습니다.")
            except Exception as e:
                logger.warning(f"SparkSession 정리 중 오류: {e}")
