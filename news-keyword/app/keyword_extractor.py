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
    
    def extract_keywords_with_pandas(self, company_name: str, start_date: str, end_date: str, top_keywords: int) -> Dict:
        """
        pandas를 사용한 키워드 추출 (백업 방법)
        여러 CSV 파일을 읽어서 통합 처리
        """
        logger.info("🐼 Pandas 엔진으로 키워드 추출을 시작합니다.")
        
        # CSV 파일들 경로 찾기
        csv_files = self.find_csv_files(start_date, end_date)
        
        all_dataframes = []
        total_loaded_rows = 0
        
        # 모든 CSV 파일 읽기
        for csv_path in csv_files:
            try:
                logger.info(f"CSV 파일 읽는 중: {os.path.basename(csv_path)}")
                df = pd.read_csv(csv_path, encoding='utf-8')
                all_dataframes.append(df)
                total_loaded_rows += len(df)
                logger.info(f"  - 로드된 행 수: {len(df)}")
            except Exception as e:
                logger.warning(f"CSV 파일 읽기 실패: {csv_path}, 오류: {e}")
                continue
        
        if not all_dataframes:
            raise FileNotFoundError("읽을 수 있는 CSV 파일이 없습니다.")
        
        # 모든 데이터프레임 병합
        df = pd.concat(all_dataframes, ignore_index=True)
        logger.info(f"총 {len(csv_files)}개 파일에서 {total_loaded_rows}개 행 로드 완료")
        logger.info(f"병합 후 총 {len(df)}개 행")
        logger.info(f"컬럼명: {list(df.columns)}")
        
        # 기업 필터링 (기관 컬럼에서 해당 기업이 포함된 행들을 가져옴)
        if '기관' in df.columns:
            # 기관 컬럼에 NaN이 아니고 회사명이 포함된 행 필터링
            mask = df['기관'].notna() & df['기관'].str.contains(company_name, na=False, regex=False)
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
                    "message": f"🐼 Pandas 엔진으로 성공적으로 키워드를 추출했습니다. 총 {len(keywords_dict)}개 키워드 발견 (파일 {len(csv_files)}개 처리)"
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
                base_result['top_keywords'] = [k for k, v in original_top]
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
            result['top_keywords'] = filtered_top_keywords
            result['ai_analysis'] = analysis
            result['ai_filtered'] = True
            result['original_keyword_count'] = len(base_result['keywords'])
            result['filtered_keyword_count'] = len(filtered_keywords)
            if self.smart_filter.is_available():
                result['message'] = f"AI 필터링 완료: {len(base_result['keywords'])}개 → {len(filtered_keywords)}개 키워드 (주가 관련성 기준)"
            else:
                result['message'] = f"규칙 기반 필터링 완료: {len(base_result['keywords'])}개 → {len(filtered_keywords)}개 키워드 (주가 관련성 기준)"
            
            logger.info(f"AI 필터링 성공: {len(base_result['keywords'])}개 → {len(filtered_keywords)}개")
            logger.info(f"필터링된 주요 키워드 개수: {len(filtered_top_keywords)}개")
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
                return self.extract_keywords_with_spark(company_name, start_date, end_date, top_keywords)
            else:
                logger.info("🐼 엔진 선택: Pandas (파일 크기 15GB 미만)")
                return self.extract_keywords_with_pandas(company_name, start_date, end_date, top_keywords)
                
        except Exception as e:
            logger.error(f"키워드 추출 중 오류 발생: {e}")
            # 최후의 수단으로 pandas 사용
            logger.info("⚠️ 오류 발생으로 Pandas 엔진으로 폴백합니다.")
            return self.extract_keywords_with_pandas(company_name, start_date, end_date, top_keywords)

    def extract_keywords_with_spark(self, company_name: str, start_date: str, end_date: str, top_keywords: int) -> Dict:
        """
        PySpark를 사용한 키워드 추출 (대용량 데이터용)
        """
        try:
            # SparkSession 초기화 시도
            self.initialize_spark()
            
            if self.spark is None:
                logger.warning("⚠️ PySpark 초기화 실패, Pandas 엔진으로 폴백합니다.")
                return self.extract_keywords_with_pandas(company_name, start_date, end_date, top_keywords)
            
            # CSV 파일들 경로 찾기
            csv_files = self.find_csv_files(start_date, end_date)
            
            logger.info(f"🚀 PySpark 엔진으로 CSV 파일들 읽기 시작: {len(csv_files)}개 파일")
            
            # 모든 CSV 파일 읽기 및 병합
            dataframes = []
            for csv_path in csv_files:
                try:
                    logger.info(f"파일 읽는 중: {os.path.basename(csv_path)}")
                    temp_df = self.spark.read \
                        .option("header", "true") \
                        .option("inferSchema", "true") \
                        .option("encoding", "UTF-8") \
                        .option("multiline", "true") \
                        .option("escape", '"') \
                        .csv(csv_path)
                    
                    dataframes.append(temp_df)
                    logger.info(f"  - 파일 로드 완료: {os.path.basename(csv_path)}")
                except Exception as e:
                    logger.warning(f"파일 읽기 실패: {csv_path}, 오류: {e}")
                    continue
            
            if not dataframes:
                raise FileNotFoundError("읽을 수 있는 CSV 파일이 없습니다.")
            
            # 모든 데이터프레임 병합
            df = dataframes[0]
            for temp_df in dataframes[1:]:
                df = df.union(temp_df)
            
            # 데이터 캐싱 (성능 향상)
            df.cache()
            
            total_rows = df.count()
            logger.info(f"총 {len(csv_files)}개 파일에서 {total_rows}개 행 로드 완료")
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
                    
                    logger.info(f"🚀 PySpark 엔진으로 키워드 추출 완료: {len(keywords_dict)}개 키워드")
                    
                    return {
                        "company_name": company_name,
                        "period": f"{start_date}-{end_date}",
                        "total_news_count": total_count,
                        "keywords": keywords_dict,
                        "top_keywords": top_keywords_list,
                        "message": f"PySpark로 성공적으로 키워드를 추출했습니다. 총 {len(keywords_dict)}개 키워드 발견 (파일 {len(csv_files)}개 처리)"
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
