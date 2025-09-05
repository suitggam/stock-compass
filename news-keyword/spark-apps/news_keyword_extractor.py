#!/usr/bin/env python3
"""
뉴스 데이터 키워드 추출 Spark 애플리케이션
기간별 기업 키워드 추출을 수행합니다.
CSV 파일 형식에 최적화된 버전
"""

import sys
import argparse
from datetime import datetime
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, regexp_replace, lower, split, explode, count, collect_list, when, isnan, isnull, to_date, date_format, substring, trim, length, size, slice
from pyspark.sql.types import StringType, ArrayType, IntegerType
import re

def clean_text(text):
    """텍스트 전처리 함수"""
    if text is None:
        return ""
    
    # HTML 태그 제거
    text = re.sub(r'<[^>]+>', '', text)
    # 특수문자 제거 (한글, 영문, 숫자, 공백만 유지)
    text = re.sub(r'[^가-힣a-zA-Z0-9\s]', ' ', text)
    # 연속된 공백을 하나로 변경
    text = re.sub(r'\s+', ' ', text)
    # 앞뒤 공백 제거
    text = text.strip()
    
    return text

def extract_keywords(text, min_length=2):
    """키워드 추출 함수"""
    if not text:
        return []
    
    # 단어 분리 (공백 기준)
    words = text.split()
    
    # 길이가 min_length 이상인 단어만 필터링
    keywords = [word for word in words if len(word) >= min_length]
    
    return keywords

def main():
    parser = argparse.ArgumentParser(description='뉴스 키워드 추출 애플리케이션 (CSV 버전)')
    parser.add_argument('--input-path', required=True, help='HDFS 입력 파일 경로')
    parser.add_argument('--output-path', required=True, help='HDFS 출력 파일 경로')
    parser.add_argument('--company-filter', help='특정 기업명 필터 (쉼표로 구분)')
    parser.add_argument('--start-date', help='시작 날짜 (YYYYMMDD)')
    parser.add_argument('--end-date', help='종료 날짜 (YYYYMMDD)')
    parser.add_argument('--min-keyword-length', type=int, default=2, help='최소 키워드 길이')
    parser.add_argument('--top-keywords', type=int, default=20, help='상위 키워드 개수')
    parser.add_argument('--use-existing-keywords', action='store_true', help='기존 키워드 컬럼 사용')
    
    args = parser.parse_args()
    
    # SparkSession 생성
    spark = SparkSession.builder \
        .appName("NewsKeywordExtractor") \
        .config("spark.sql.adaptive.enabled", "true") \
        .config("spark.sql.adaptive.coalescePartitions.enabled", "true") \
        .getOrCreate()
    
    try:
        print(f"입력 파일 경로: {args.input_path}")
        print(f"출력 파일 경로: {args.output_path}")
        
        # CSV 파일 읽기
        df = spark.read \
            .option("header", "true") \
            .option("inferSchema", "true") \
            .option("encoding", "UTF-8") \
            .csv(args.input_path)
        
        print(f"데이터 로드 완료. 총 {df.count()}개 행")
        print("컬럼명:", df.columns)
        
        # 날짜 필터링 (일자 컬럼 사용)
        if '일자' in df.columns:
            # 날짜 형식 변환 (20220905 -> 2022-09-05)
            df = df.withColumn('date_formatted', 
                             date_format(to_date(col('일자'), 'yyyyMMdd'), 'yyyy-MM-dd'))
            
            if args.start_date:
                start_date_formatted = f"{args.start_date[:4]}-{args.start_date[4:6]}-{args.start_date[6:8]}"
                df = df.filter(col('date_formatted') >= start_date_formatted)
            if args.end_date:
                end_date_formatted = f"{args.end_date[:4]}-{args.end_date[4:6]}-{args.end_date[6:8]}"
                df = df.filter(col('date_formatted') <= end_date_formatted)
        
        # 기업 필터링 (기관 컬럼 사용) - 부분 매칭 지원
        if args.company_filter and '기관' in df.columns:
            companies = [c.strip() for c in args.company_filter.split(',')]
            # 각 기업이 기관 컬럼에 포함되는지 확인 (부분 매칭)
            filter_condition = None
            for company in companies:
                if filter_condition is None:
                    filter_condition = col('기관').contains(company)
                else:
                    filter_condition = filter_condition | col('기관').contains(company)
            df = df.filter(filter_condition)
        
        print(f"필터링 후 데이터: {df.count()}개 행")
        
        if args.use_existing_keywords and '키워드' in df.columns:
            # 기존 키워드 컬럼 사용
            print("기존 키워드 컬럼을 사용합니다.")
            
            # 키워드 분리 및 정리
            keywords_df = df.select(
                col('일자').alias('date'),
                col('기관').alias('company'),
                col('제목').alias('title'),
                explode(split(col('키워드'), ',')).alias('keyword')
            ).filter(col('keyword') != '')
            
            # 키워드 정리
            keywords_df = keywords_df.withColumn('keyword', 
                regexp_replace(trim(col('keyword')), r'[^가-힣a-zA-Z0-9\s]', ''))
            keywords_df = keywords_df.filter(length(col('keyword')) >= args.min_keyword_length)
            
        else:
            # 제목과 본문에서 키워드 추출
            print("제목과 본문에서 키워드 추출합니다.")
            
            # 제목 키워드 추출
            title_keywords = df.select(
                col('일자').alias('date'),
                col('기관').alias('company'),
                col('제목').alias('title'),
                explode(split(regexp_replace(lower(col('제목')), r'[^가-힣a-zA-Z0-9\s]', ' '), ' ')).alias('keyword')
            ).filter(col('keyword') != '').filter(length(col('keyword')) >= args.min_keyword_length)
            
            # 본문 키워드 추출 (있는 경우)
            if '본문' in df.columns:
                content_keywords = df.select(
                    col('일자').alias('date'),
                    col('기관').alias('company'),
                    col('제목').alias('title'),
                    explode(split(regexp_replace(lower(col('본문')), r'[^가-힣a-zA-Z0-9\s]', ' '), ' ')).alias('keyword')
                ).filter(col('keyword') != '').filter(length(col('keyword')) >= args.min_keyword_length)
                
                keywords_df = title_keywords.union(content_keywords)
            else:
                keywords_df = title_keywords
        
        # 기간별 기업 키워드 분석
        print("기간별 기업 키워드 분석 중...")
        
        # 날짜별 그룹화를 위한 날짜 포맷팅
        keywords_df = keywords_df.withColumn('date_formatted', 
            date_format(to_date(col('date'), 'yyyyMMdd'), 'yyyy-MM-dd'))
        
        # 기간별 기업 키워드 빈도 계산
        keyword_freq = keywords_df.groupBy('date_formatted', 'company', 'keyword') \
            .agg(count('*').alias('frequency')) \
            .orderBy(col('date_formatted'), col('company'), col('frequency').desc())
        
        # 기간별 기업별 상위 키워드 추출 (문자열로 변환)
        top_keywords_by_period = keyword_freq.groupBy('date_formatted', 'company') \
            .agg(collect_list('keyword').alias('keywords'),
                 collect_list('frequency').alias('frequencies')) \
            .withColumn('top_keywords', 
                when(size(col('keywords')) > args.top_keywords, 
                     slice(col('keywords'), 1, args.top_keywords))
                .otherwise(col('keywords'))) \
            .withColumn('top_frequencies',
                when(size(col('frequencies')) > args.top_keywords,
                     slice(col('frequencies'), 1, args.top_keywords))
                .otherwise(col('frequencies'))) \
            .withColumn('top_keywords_str', 
                regexp_replace(col('top_keywords').cast('string'), r'[\[\]]', '')) \
            .withColumn('top_frequencies_str', 
                regexp_replace(col('top_frequencies').cast('string'), r'[\[\]]', ''))
        
        # 전체 기간 기업별 키워드 요약 (문자열로 변환)
        company_keywords_summary = keywords_df.groupBy('company', 'keyword') \
            .agg(count('*').alias('total_frequency')) \
            .orderBy(col('company'), col('total_frequency').desc())
        
        company_top_keywords = company_keywords_summary.groupBy('company') \
            .agg(collect_list('keyword').alias('all_keywords'),
                 collect_list('total_frequency').alias('all_frequencies')) \
            .withColumn('top_keywords', 
                when(size(col('all_keywords')) > args.top_keywords,
                     slice(col('all_keywords'), 1, args.top_keywords))
                .otherwise(col('all_keywords'))) \
            .withColumn('top_frequencies',
                when(size(col('all_frequencies')) > args.top_keywords,
                     slice(col('all_frequencies'), 1, args.top_keywords))
                .otherwise(col('all_frequencies'))) \
            .withColumn('top_keywords_str', 
                regexp_replace(col('top_keywords').cast('string'), r'[\[\]]', '')) \
            .withColumn('top_frequencies_str', 
                regexp_replace(col('top_frequencies').cast('string'), r'[\[\]]', ''))
        
        # 결과 저장
        print("결과 저장 중...")
        
        # 기간별 상세 결과 (문자열 컬럼만 사용)
        top_keywords_by_period.select(
            'date_formatted', 'company', 'top_keywords_str', 'top_frequencies_str'
        ).write \
            .mode('overwrite') \
            .option('header', 'true') \
            .csv(f"{args.output_path}/period_analysis")
        
        # 기업별 요약 결과 (문자열 컬럼만 사용)
        company_top_keywords.select(
            'company', 'top_keywords_str', 'top_frequencies_str'
        ).write \
            .mode('overwrite') \
            .option('header', 'true') \
            .csv(f"{args.output_path}/company_summary")
        
        # 키워드 빈도 상세 결과
        keyword_freq.write \
            .mode('overwrite') \
            .option('header', 'true') \
            .csv(f"{args.output_path}/keyword_frequency")
        
        print(f"키워드 추출 완료. 결과가 {args.output_path}에 저장되었습니다.")
        
        # 결과 미리보기
        print("\n=== 기간별 키워드 분석 결과 미리보기 ===")
        top_keywords_by_period.select(
            'date_formatted', 'company', 'top_keywords_str', 'top_frequencies_str'
        ).show(10, truncate=False)
        
        print("\n=== 기업별 키워드 요약 결과 미리보기 ===")
        company_top_keywords.select(
            'company', 'top_keywords_str', 'top_frequencies_str'
        ).show(10, truncate=False)
        
    except Exception as e:
        print(f"오류 발생: {str(e)}")
        raise
    finally:
        spark.stop()

if __name__ == "__main__":
    main()
