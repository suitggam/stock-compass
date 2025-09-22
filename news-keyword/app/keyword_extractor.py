from typing import Optional, Dict, List
import os
import logging
import pandas as pd
import re
import glob
import boto3
from datetime import datetime, timedelta
from collections import Counter
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, split, explode, count, collect_list, when, size, slice, regexp_replace, trim, length, lower
from smart_keyword_filter import SmartKeywordFilter
from spark_analyzer import SparkAnalyzer
from pandas_analyzer import PandasAnalyzer

# 로깅 설정
logger = logging.getLogger(__name__)

class KeywordExtractor:
    """PySpark를 사용한 키워드 추출 클래스"""
    
    def __init__(self):
        self.spark = None
        self.csv_file_path = None
        self.smart_filter = SmartKeywordFilter()
        
        # S3 설정
        self.s3_bucket = os.getenv('S3_BUCKET', 'cheesecrust-spark-data-bucket')
        self.s3_prefix = os.getenv('S3_PREFIX', 'outputs/data/')
        self.s3_region = os.getenv('AWS_DEFAULT_REGION', 'ap-northeast-2')
        
        # S3 클라이언트 초기화
        self.s3_client = boto3.client(
            's3',
            region_name=self.s3_region,
            aws_access_key_id=os.getenv('AWS_ACCESS_KEY_ID'),
            aws_secret_access_key=os.getenv('AWS_SECRET_ACCESS_KEY'),
            aws_session_token=os.getenv('AWS_SESSION_TOKEN')
        )
        
        # 분석기 초기화
        self.pandas_analyzer = PandasAnalyzer()
        self.spark_analyzer = None  # Spark 초기화 후 설정
        
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
                    .config("spark.hadoop.fs.s3a.access.key", os.getenv('AWS_ACCESS_KEY_ID', '')) \
                    .config("spark.hadoop.fs.s3a.secret.key", os.getenv('AWS_SECRET_ACCESS_KEY', '')) \
                    .config("spark.hadoop.fs.s3a.session.token", os.getenv('AWS_SESSION_TOKEN', '')) \
                    .config("spark.hadoop.fs.s3a.endpoint", f"s3.{self.s3_region}.amazonaws.com") \
                    .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem") \
                    .config("spark.hadoop.fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.auth.TemporaryAWSCredentialsProvider") \
                    .getOrCreate()
                
                # 로그 레벨 설정 (너무 많은 로그 방지)
                self.spark.sparkContext.setLogLevel("WARN")
                
                # Java 버전 확인
                java_version = self.spark.sparkContext._jvm.System.getProperty("java.version")
                logger.info(f"SparkSession 초기화 성공! Java 버전: {java_version}")
                
                # SparkAnalyzer 초기화
                self.spark_analyzer = SparkAnalyzer(self.spark, self.s3_bucket, self.s3_prefix)
                
            except Exception as e:
                logger.error(f"SparkSession 초기화 실패: {e}")
                logger.info("Java 버전 호환성 문제일 가능성이 높습니다.")
                # pandas 백업 플랜 사용을 위해 spark를 None으로 유지
                self.spark = None
                raise
    
    def find_csv_files(self, start_date: str, end_date: str) -> List[str]:
        """
        날짜 범위에 해당하는 CSV 파일들을 S3에서 찾습니다.
        S3 버킷에서 해당 기간의 모든 CSV 파일을 반환합니다.
        """
        
        # 입력 날짜를 datetime 객체로 변환
        start_dt = datetime.strptime(start_date, "%Y%m%d")
        end_dt = datetime.strptime(end_date, "%Y%m%d")
        
        matching_files = []
        
        try:
            # S3에서 객체 목록 가져오기
            paginator = self.s3_client.get_paginator('list_objects_v2')
            page_iterator = paginator.paginate(
                Bucket=self.s3_bucket,
                Prefix=self.s3_prefix
            )
            
            for page in page_iterator:
                if 'Contents' in page:
                    for obj in page['Contents']:
                        key = obj['Key']
                        
                        # CSV 파일만 필터링
                        if key.endswith('.csv'):
                            filename = os.path.basename(key)
                            
                            try:
                                # NewsResult_YYYYMMDD-YYYYMMDD.csv 형식에서 날짜 추출
                                if filename.startswith('NewsResult_'):
                                    date_part = filename.replace("NewsResult_", "").replace(".csv", "")
                                    
                                    if "-" in date_part:
                                        file_start_str, file_end_str = date_part.split("-")
                                        file_start_dt = datetime.strptime(file_start_str, "%Y%m%d")
                                        file_end_dt = datetime.strptime(file_end_str, "%Y%m%d")
                                        
                                        # 날짜 범위가 겹치는지 확인
                                        if (file_start_dt <= end_dt and file_end_dt >= start_dt):
                                            s3_path = f"s3a://{self.s3_bucket}/{key}"
                                            matching_files.append(s3_path)
                                            logger.info(f"매칭된 S3 파일: {filename} ({file_start_str}-{file_end_str})")
                                            
                            except ValueError as e:
                                # 날짜 파싱 실패 시 건너뛰기
                                logger.debug(f"날짜 파싱 실패, 파일 건너뛰기: {filename}")
                                continue
                                
        except Exception as e:
            logger.error(f"S3에서 파일 목록을 가져오는 중 오류 발생: {e}")
            raise FileNotFoundError(f"S3에서 파일을 찾을 수 없습니다: {e}")
        
        if not matching_files:
            raise FileNotFoundError(f"날짜 범위 {start_date}-{end_date}에 해당하는 CSV 파일을 S3에서 찾을 수 없습니다.")
        
        matching_files.sort()  # 파일명 순으로 정렬
        logger.info(f"총 {len(matching_files)}개의 CSV 파일을 S3에서 찾았습니다.")
        
        return matching_files
    

    def extract_smart_keywords_from_csv(self, company_name: str, start_date: str, end_date: str, top_keywords: int, use_ai_filter: bool = True) -> Dict:
        """
        CSV 파일에서 특정 기업의 키워드를 추출하고 AI 필터링을 적용합니다.
        
        Args:
            company_name: 기업명
            start_date: 시작 날짜 (YYYYMMDD)
            end_date: 종료 날짜 (YYYYMMDD)
            top_keywords: 상위 키워드 개수
            use_ai_filter: AI 필터링 사용 여부
        """
        # 기본 키워드 추출
        base_result = self.extract_keywords_from_csv(company_name, start_date, end_date, top_keywords * 2)  # 더 많은 키워드 추출
        
        if not use_ai_filter or not base_result.get('keywords'):
            return base_result
        
        try:
            logger.info(f"AI 필터링 시작: {len(base_result['keywords'])}개 키워드")
            
            # SmartKeywordFilter 가용성 확인
            if not self.smart_filter.is_available():
                logger.warning("OpenAI API를 사용할 수 없습니다. .env 파일의 OPENAI_API_KEY를 확인해주세요.")
                base_result['ai_filtered'] = False
                base_result['ai_analysis'] = "OpenAI API 키가 설정되지 않았습니다."
                base_result['message'] += " (AI 필터링 사용 불가)"
                return base_result
            
            # AI 필터링 적용
            filtered_keywords, filtered_top_keywords = self.smart_filter.filter_stock_related_keywords(
                base_result['keywords'], 
                company_name, 
                top_keywords
            )
            
            # 필터링 결과 검증
            if not filtered_keywords:
                logger.warning("AI 필터링 결과가 비어있습니다. 원본 키워드를 반환합니다.")
                # 원본 키워드의 상위 키워드만 반환
                original_top = list(base_result['keywords'].items())[:top_keywords]
                base_result['keywords'] = dict(original_top)
                base_result['ai_filtered'] = False
                base_result['ai_analysis'] = "AI 필터링에서 유효한 키워드를 찾지 못했습니다."
                base_result['original_keyword_count'] = len(base_result['keywords'])
                base_result['filtered_keyword_count'] = 0
                base_result['message'] += " (AI 필터링 결과 없음)"
                return base_result
            
            # 키워드 분석 추가
            analysis = ""
            if self.smart_filter.is_available() and filtered_keywords:
                try:
                    analysis = self.smart_filter.get_keyword_analysis(filtered_keywords, company_name)
                except Exception as e:
                    logger.warning(f"AI 분석 중 오류: {e}")
                    analysis = "키워드 분석을 수행할 수 없습니다."
            
            # 결과 업데이트
            result = base_result.copy()
            result['keywords'] = filtered_keywords
            result['ai_analysis'] = analysis
            result['ai_filtered'] = True
            result['original_keyword_count'] = len(base_result['keywords'])
            result['filtered_keyword_count'] = len(filtered_keywords)
            
            # 뉴스 기사 정보는 필터링된 키워드로 다시 추출
            if 'top_news_articles' in base_result and filtered_top_keywords:
                # 필터링된 키워드로 뉴스 기사 재추출
                result['top_news_articles'] = self.re_extract_news_articles_with_filtered_keywords(
                    base_result['top_news_articles'], filtered_top_keywords
                )
            if self.smart_filter.is_available():
                result['message'] = f"AI 필터링 완료: {len(base_result['keywords'])}개 → {len(filtered_keywords)}개 키워드 (주가 관련성 기준)"
            else:
                result['message'] = f"규칙 기반 필터링 완료: {len(base_result['keywords'])}개 → {len(filtered_keywords)}개 키워드 (주가 관련성 기준)"
            
            logger.info(f"AI 필터링 성공: {len(base_result['keywords'])}개 → {len(filtered_keywords)}개")
            return result
            
        except Exception as e:
            logger.error(f"AI 필터링 중 오류 발생: {e}")
            # AI 필터링 실패 시 원본 결과 반환
            base_result['ai_filtered'] = False
            base_result['ai_analysis'] = "AI 필터링을 사용할 수 없습니다."
            base_result['message'] += " (AI 필터링 실패로 원본 키워드 반환)"
            return base_result

    def get_total_file_size(self, csv_files: List[str]) -> int:
        """S3에서 파일들의 총 크기를 계산합니다 (바이트 단위)"""
        total_size = 0
        try:
            for csv_path in csv_files:
                # s3a://bucket/path/file.csv -> bucket/path/file.csv
                s3_key = csv_path.replace(f"s3a://{self.s3_bucket}/", "")
                
                response = self.s3_client.head_object(
                    Bucket=self.s3_bucket,
                    Key=s3_key
                )
                file_size = response['ContentLength']
                total_size += file_size
                logger.info(f"파일 크기: {os.path.basename(csv_path)} - {file_size / (1024**3):.2f} GB")
                
        except Exception as e:
            logger.warning(f"파일 크기 계산 실패: {e}")
            return 0
            
        return total_size

    def extract_keywords_from_csv(self, company_name: str, start_date: str, end_date: str, top_keywords: int) -> Dict:
        """
        CSV 파일에서 특정 기업의 키워드를 추출합니다.
        파일 크기에 따라 Spark 또는 Pandas를 자동 선택합니다.
        """
        try:
            # CSV 파일들 경로 찾기
            csv_files = self.find_csv_files(start_date, end_date)
            
            # 파일 크기 계산
            total_size = self.get_total_file_size(csv_files)
            total_size_gb = total_size / (1024**3)
            
            logger.info(f"총 파일 크기: {total_size_gb:.2f} GB")
            
            # 15GB 이상이면 Spark 사용
            if total_size_gb >= 15.0:
                logger.info("🚀 엔진 선택: PySpark (파일 크기 15GB 이상)")
                # Spark 초기화 시도
                try:
                    self.initialize_spark()
                    if self.spark_analyzer is None:
                        raise Exception("SparkAnalyzer 초기화 실패")
                    return self.spark_analyzer.extract_keywords_with_spark(company_name, start_date, end_date, top_keywords, csv_files)
                except Exception as e:
                    logger.warning(f"⚠️ PySpark 실행 실패: {e}, Pandas로 폴백합니다.")
                    return self.pandas_analyzer.extract_keywords_with_pandas(company_name, start_date, end_date, top_keywords, csv_files)
            else:
                logger.info("🐼 엔진 선택: Pandas (파일 크기 15GB 미만)")
                return self.pandas_analyzer.extract_keywords_with_pandas(company_name, start_date, end_date, top_keywords, csv_files)
                
        except Exception as e:
            logger.error(f"키워드 추출 중 오류 발생: {e}")
            # 최후의 수단으로 pandas 사용
            logger.info("⚠️ 오류 발생으로 Pandas 엔진으로 폴백합니다.")
            return self.pandas_analyzer.extract_keywords_with_pandas(company_name, start_date, end_date, top_keywords, csv_files)

    def re_extract_news_articles_with_filtered_keywords(self, original_articles, filtered_keywords):
        """
        AI 필터링된 키워드로 뉴스 기사들을 재추출합니다.
        
        Args:
            original_articles: 원본 뉴스 기사 리스트
            filtered_keywords: AI 필터링된 키워드 리스트
            
        Returns:
            List[Dict]: 필터링된 키워드와 매칭되는 뉴스 기사 리스트
        """
        try:
            if not original_articles or not filtered_keywords:
                return []
            
            # 필터링된 키워드와 매칭되는 기사들만 추출
            filtered_articles = []
            
            for article in original_articles:
                # 기사의 키워드와 필터링된 키워드 매칭 확인
                matched_count = 0
                matched_keywords = []
                
                for filtered_keyword in filtered_keywords:
                    for article_keyword in article.get('all_keywords', []):
                        if filtered_keyword in article_keyword or article_keyword in filtered_keyword:
                            matched_count += 1
                            matched_keywords.append(filtered_keyword)
                            break  # 중복 카운트 방지
                
                if matched_count > 0:
                    # 기사 정보 업데이트
                    updated_article = article.copy()
                    updated_article['matched_keywords_count'] = matched_count
                    updated_article['matched_keywords'] = list(set(matched_keywords))
                    filtered_articles.append(updated_article)
            
            # 매칭된 키워드 개수 순으로 정렬
            filtered_articles.sort(key=lambda x: x['matched_keywords_count'], reverse=True)
            
            logger.info(f"AI 필터링된 키워드로 {len(filtered_articles)}개 뉴스 기사 재추출 완료")
            
            return filtered_articles
            
        except Exception as e:
            logger.warning(f"뉴스 기사 재추출 중 오류: {e}")
            return original_articles  # 오류 시 원본 반환
    
    def cleanup(self):
        """SparkSession 정리"""
        if self.spark:
            try:
                self.spark.stop()
                self.spark = None
                logger.info("SparkSession이 정리되었습니다.")
            except Exception as e:
                logger.warning(f"SparkSession 정리 중 오류: {e}")
