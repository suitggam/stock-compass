#!/usr/bin/env python3
"""
단순화된 KOSPI 200 기업 대상 Docker Spark 클러스터용 PageRank 분석기
- 직렬화 문제 완전 해결
- 사용법: docker exec -it spark-client python /opt/spark/jobs/spark_pageRank_kospi200_simple.py
"""

from pyspark.sql import SparkSession
import pyspark.sql.functions as F
from pyspark.sql.types import *
from pyspark.sql.window import Window
import pandas as pd
import numpy as np
import networkx as nx
import os
import glob
import sys
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

# 글로벌 KOSPI 200 기업 리스트 (클래스 외부에 정의)
KOSPI200_COMPANIES = [
    "BGF리테일","BNK금융지주","CJ","CJ대한통운","CJ제일제당",
    "DB손해보험","DL","DL이앤씨","DN오토모티브","F&F",
    "GKL","GS","GS건설","GS리테일","HDC",
    "HD한국조선해양","HD현대","HD현대마린솔루션","HD현대미포","HD현대인프라코어",
    "HD현대일렉트릭","HD현대중공업","HL만도","HMM","HS효성첨단소재",
    "JB금융지주","KB금융","KCC","KG모빌리티","KT",
    "KT&G",
    "LG","LG디스플레이","LG생활건강","LG에너지솔루션","LG유플러스",
    "LG이노텍","LG전자","LG화학","LIG넥스원","LS",
    "LS ELECTRIC","NAVER","NH투자증권","OCI","OCI홀딩스",
    "POSCO홀딩스","S-Oil","SK","SKC","SK바이오사이언스",
    "SK바이오팜","SK스퀘어","SK아이이테크놀로지","SK이노베이션","SK케미칼",
    "SK텔레콤","SK하이닉스","TCC스틸","TKG휴켐스","iM금융지주",
    "강원랜드","고려아연","금호석유화학","금호타이어","기아",
    "기업은행","넷마블","녹십자","녹십자홀딩스","농심",
    "대상","대우건설","대웅","대웅제약","대한유화",
    "대한전선","대한항공","더블유게임즈","덴티움","동서",
    "동원산업","동원시스템즈","두산","두산로보틱스","두산밥캣",
    "두산에너빌리티","롯데쇼핑","롯데웰푸드","롯데정밀화학","롯데지주",
    "롯데칠성","롯데케미칼","메리츠금융지주","미래에셋증권","미스토홀딩스",
    "미원상사","미원에스씨","삼성E&A","삼성SDI","삼성물산",
    "삼성바이오로직스","삼성생명","삼성에스디에스","삼성전기","삼성전자",
    "삼성중공업","삼성증권","삼성카드","삼성화재","삼양식품",
    "세방전지","세아베스틸지주","세아제강지주","셀트리온","신세계",
    "신한지주","씨에스윈드","아모레퍼시픽","아모레퍼시픽홀딩스","에스디바이오센서",
    "에스엘","에스원","에이피알","에코프로머티","엔씨소프트",
    "엘앤에프","영원무역","영원무역홀딩스","영풍","오뚜기",
    "오리온","오리온홀딩스","우리금융지주","유한양행","율촌화학",
    "이마트","이수스페셜티케미컬","제일기획","종근당","지역난방공사",
    "카카오","카카오뱅크","카카오페이","코스맥스","코스모화학",
    "코오롱인더","코웨이","크래프톤","키움증권","태광산업",
    "팬오션","포스코DX","포스코인터내셔널","포스코퓨처엠","풍산",
    "하나금융지주","하나투어","하이브","하이트진로","한국가스공사",
    "한국금융지주","한국앤컴퍼니","한국전력","한국카본","한국콜마",
    "한국타이어앤테크놀로지","한국항공우주","한미반도체","한미사이언스","한미약품",
    "한샘","한솔케미칼","한온시스템","한올바이오파마","한일시멘트",
    "한전KPS","한전기술","한진칼","한화","한화비전",
    "한화생명","한화솔루션","한화시스템","한화에어로스페이스","한화오션",
    "현대건설","현대글로비스","현대로템","현대모비스","현대백화점",
    "현대엘리베이터","현대위아","현대제철","현대차","현대해상",
    "호텔신라","효성중공업","효성티앤씨","후성"
]

def init_spark_session():
    """Docker Spark 클러스터 세션 초기화"""
    try:
        print("🚀 Docker Spark 클러스터 연결 중...")
        
        spark = SparkSession.builder \
            .appName("SimpleKOSPI200PageRank") \
            .master("spark://spark-master:7077") \
            .config("spark.sql.adaptive.enabled", "true") \
            .config("spark.sql.adaptive.coalescePartitions.enabled", "true") \
            .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
            .config("spark.sql.execution.arrow.pyspark.enabled", "false") \
            .config("spark.network.timeout", "800s") \
            .config("spark.sql.broadcastTimeout", "600") \
            .getOrCreate()
        
        print(f"✅ Docker Spark 클러스터 연결 완료!")
        print(f"   Spark 버전: {spark.version}")
        
        return spark
        
    except Exception as e:
        print(f"❌ Docker Spark 클러스터 연결 실패: {e}")
        sys.exit(1)

def load_csv_data(spark, csv_file):
    """CSV 파일을 Spark DataFrame으로 로드"""
    
    print("📁 CSV 파일 로드 중...")
    print("=" * 50)
    
    try:
        news_df = spark.read \
            .option("header", "true") \
            .option("inferSchema", "true") \
            .option("encoding", "UTF-8") \
            .csv(csv_file)
        
        news_df.cache()
        
        total_count = news_df.count()
        column_count = len(news_df.columns)
        
        print(f"✅ 파일 로드 성공!")
        print(f"   전체 뉴스: {total_count:,}건")
        print(f"   컬럼 수: {column_count}개")
        
        return news_df
        
    except Exception as e:
        print(f"❌ 파일 로드 실패: {e}")
        return None

def extract_kospi200_connections(spark, news_df):
    """KOSPI 200 기업 간 연결 관계 추출 (완전 함수형)"""
    
    print(f"\n🔗 KOSPI 200 기업 간 연결 관계 추출")
    print("=" * 50)
    
    # '기관' 컬럼 찾기
    org_column = None
    columns = news_df.columns
    
    for column_name in columns:
        if '기관' in column_name or 'company' in column_name.lower() or 'organization' in column_name.lower():
            org_column = column_name
            break
    
    if org_column is None:
        print("❌ '기관' 컬럼을 찾을 수 없습니다!")
        return None
    
    print(f"✅ 기관 컬럼 발견: '{org_column}'")
    
    try:
        # 1단계: 기본 필터링
        print("🔄 1단계: 기본 데이터 필터링...")
        valid_news = news_df.filter(
            F.col(org_column).isNotNull() & 
            (F.col(org_column) != "") &
            (F.length(F.col(org_column)) > 1)
        )
        
        valid_count = valid_news.count()
        print(f"   유효한 뉴스: {valid_count:,}건")
        
        if valid_count == 0:
            print("❌ 유효한 기관 정보가 없습니다!")
            return None
        
        # 2단계: 뉴스별 ID 추가하여 같은 뉴스 내 기업들 그룹핑
        print("🔄 2단계: 뉴스별 ID 추가...")
        news_with_id = valid_news.select(
            F.monotonically_increasing_id().alias("news_id"),
            F.col(org_column).alias("companies")
        )
        
        # 3단계: 기업명 분리
        print("🔄 3단계: 기업명 분리...")
        companies_exploded = news_with_id.select(
            "news_id",
            F.explode(F.split(F.col("companies"), ",")).alias("company")
        ).select(
            "news_id",
            F.trim(F.col("company")).alias("company")
        ).filter(
            (F.col("company") != "") & 
            (F.length(F.col("company")) > 1)
        )
        
        # 4단계: KOSPI 200 기업만 필터링 (IN 연산자 사용)
        print("🔄 4단계: KOSPI 200 기업 필터링...")
        kospi200_companies_filtered = companies_exploded.filter(
            F.col("company").isin(KOSPI200_COMPANIES)
        )
        
        kospi_count = kospi200_companies_filtered.select("company").distinct().count()
        print(f"   발견된 KOSPI 200 기업: {kospi_count}개")
        
        if kospi_count == 0:
            print("❌ 데이터에서 KOSPI 200 기업을 찾을 수 없습니다!")
            return None
        
        # 5단계: 같은 뉴스 내에서 기업 간 연결 생성
        print("🔄 5단계: 기업 간 연결 관계 생성...")
        
        # Self-join으로 같은 뉴스 내 기업 쌍 생성
        connections = kospi200_companies_filtered.alias("c1").join(
            kospi200_companies_filtered.alias("c2"),
            (F.col("c1.news_id") == F.col("c2.news_id")) & 
            (F.col("c1.company") < F.col("c2.company"))  # 중복 제거 및 순서 정렬
        ).select(
            F.col("c1.company").alias("company1"),
            F.col("c2.company").alias("company2")
        )
        
        # 6단계: 연결 강도 계산
        print("🔄 6단계: 연결 강도 계산...")
        connections_df = connections.groupBy("company1", "company2") \
            .count() \
            .withColumnRenamed("count", "weight") \
            .filter(F.col("weight") > 0)
        
        connections_df.cache()
        
        # 통계
        connection_count = connections_df.count()
        
        if connection_count == 0:
            print("❌ KOSPI 200 기업 간 연결 관계를 찾을 수 없습니다!")
            return None
        
        avg_weight = connections_df.agg(F.avg("weight")).collect()[0][0]
        max_weight = connections_df.agg(F.max("weight")).collect()[0][0]
        
        participating_companies = connections_df.select("company1").union(
            connections_df.select("company2")
        ).distinct().count()
        
        print(f"📊 KOSPI 200 추출 결과:")
        print(f"   참여 기업 수: {participating_companies}개")
        print(f"   총 연결 관계: {connection_count:,}개")
        print(f"   평균 연결 강도: {avg_weight:.1f}회")
        print(f"   최대 연결 강도: {max_weight}회")
        
        # 상위 연결 관계 출력
        print(f"\n🔗 강한 연결 관계 TOP 5:")
        top_connections = connections_df.orderBy(F.desc("weight")).limit(5).collect()
        for i, row in enumerate(top_connections, 1):
            print(f"   {i}. {row['company1']} ↔ {row['company2']}: {row['weight']}회")
        
        return connections_df
        
    except Exception as e:
        print(f"❌ 연결 관계 추출 실패: {e}")
        import traceback
        traceback.print_exc()
        return None

def calculate_pagerank(spark, connections_df):
    """KOSPI 200 PageRank 계산 (함수형)"""
    
    print(f"\n🏆 KOSPI 200 PageRank 계산")
    print("=" * 40)
    
    try:
        # 정점 준비
        vertices = connections_df.select(F.col("company1").alias("company")).union(
            connections_df.select(F.col("company2").alias("company"))
        ).distinct()
        
        num_vertices = vertices.count()
        print(f"   분석 대상 기업 수: {num_vertices}개")
        
        edges = connections_df.select(
            F.col("company1").alias("src"),
            F.col("company2").alias("dst"),
            F.col("weight").cast("double")
        )
        
        # PageRank 파라미터
        damping = 0.85
        base_val = (1.0 - damping) / float(num_vertices)
        
        # 초기 rank
        ranks = vertices.withColumn("rank", F.lit(1.0 / float(num_vertices)))
        
        # out-degree 계산
        out_weight = edges.groupBy("src").agg(F.sum("weight").alias("out_w"))
        edges_norm = edges.join(out_weight, on="src", how="left").withColumn(
            "norm_w", F.when(F.col("out_w") > 0, F.col("weight") / F.col("out_w")).otherwise(F.lit(0.0))
        ).select("src", "dst", "norm_w")
        
        # 반복 계산
        iterations = 25
        print(f"🔄 PageRank 반복 계산 ({iterations}회)...")
        
        for i in range(iterations):
            # contribution 계산
            contribs = edges_norm.join(
                ranks.withColumnRenamed("company", "src"), on="src", how="left"
            ).withColumn(
                "contrib", F.col("rank") * F.col("norm_w")
            ).groupBy("dst").agg(F.sum("contrib").alias("sum_contrib"))
            
            # 새로운 rank 계산
            ranks = vertices.join(
                contribs.withColumnRenamed("dst", "company"), on="company", how="left"
            ).withColumn(
                "sum_contrib", F.coalesce(F.col("sum_contrib"), F.lit(0.0))
            ).withColumn(
                "rank", F.lit(base_val) + F.lit(damping) * F.col("sum_contrib")
            ).select("company", "rank")
            
            if (i + 1) % 5 == 0:
                print(f"   반복 {i + 1}/{iterations} 완료")
        
        pagerank_results = ranks.select(
            F.col("company"), F.col("rank").alias("pagerank_score")
        ).orderBy(F.desc("pagerank_score"))
        
        print(f"✅ PageRank 계산 완료!")
        
        # 결과 출력
        print(f"\n🏆 KOSPI 200 영향력 순위:")
        print(f"{'순위':>4} {'기업명':>20} {'PageRank 점수':>15} {'상대 점수':>10}")
        print("-" * 60)
        
        top_results = pagerank_results.limit(15).collect()
        max_score = top_results[0]['pagerank_score'] if top_results else 0
        
        for i, row in enumerate(top_results, 1):
            company = row['company']
            score = row['pagerank_score']
            relative = (score / max_score) * 100 if max_score > 0 else 0
            print(f"{i:>4} {company:>20} {score:>15.6f} {relative:>9.1f}%")
        
        return pagerank_results
        
    except Exception as e:
        print(f"❌ PageRank 계산 실패: {e}")
        import traceback
        traceback.print_exc()
        return None

def main():
    """메인 실행 함수 (완전 함수형)"""
    
    print("🚀 단순화된 KOSPI 200 PageRank 분석기")
    print("=" * 50)
    
    # CSV 파일 경로
    csv_files = ["/opt/spark/data/*.csv"]
    
    print("📁 CSV 파일 검색 중...")
    csv_file = None
    for file in csv_files:
        if any(ch in file for ch in ['*', '?', '[']):
            matched = glob.glob(file)
            if matched:
                csv_file = file
                print(f"✅ 매칭된 파일 {len(matched)}개: {file}")
                break
    
    if csv_file is None:
        print("❌ CSV 파일을 찾을 수 없습니다!")
        return
    
    # Spark 세션 초기화
    spark = init_spark_session()
    
    try:
        # 1. CSV 로드
        news_df = load_csv_data(spark, csv_file)
        if news_df is None:
            return
        
        # 2. 연결 관계 추출
        connections_df = extract_kospi200_connections(spark, news_df)
        if connections_df is None:
            return
        
        # 3. PageRank 계산
        pagerank_results = calculate_pagerank(spark, connections_df)
        if pagerank_results is None:
            return
        
        print(f"\n🎉 KOSPI 200 분석 완료!")
        
        total_companies = pagerank_results.count()
        print(f"\n📈 분석 요약:")
        print(f"   분석된 기업 수: {total_companies}개")
        
        top_3 = pagerank_results.limit(3).collect()
        print(f"   TOP 3 영향력 기업:")
        for i, row in enumerate(top_3, 1):
            print(f"     {i}. {row['company']} (점수: {row['pagerank_score']:.6f})")
    
    finally:
        print("🔄 Spark 세션 종료 중...")
        spark.stop()
        print("✅ 완료")

if __name__ == "__main__":
    main()