"""
PySpark를 사용한 키워드 추출 엔진
대용량 데이터 처리에 최적화
"""

import os
import re
from typing import Dict, List
from collections import Counter
import logging

logger = logging.getLogger(__name__)

class SparkAnalyzer:
    """PySpark를 사용한 키워드 추출 분석기"""
    
    def __init__(self, spark_session, s3_bucket: str, s3_prefix: str):
        self.spark = spark_session
        self.s3_bucket = s3_bucket
        self.s3_prefix = s3_prefix
    
    def extract_keywords_with_spark(self, company_name: str, start_date: str, end_date: str, top_keywords: int, csv_files: List[str]) -> Dict:
        """
        PySpark를 사용한 키워드 추출 (대용량 데이터용)
        """
        try:
            if self.spark is None:
                raise Exception("SparkSession이 초기화되지 않았습니다.")
            
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
                    logger.info(f"  - 로드된 행 수: {temp_df.count()}")
                    
                except Exception as e:
                    logger.warning(f"CSV 파일 읽기 실패: {csv_path}, 오류: {e}")
                    continue
            
            if not dataframes:
                raise FileNotFoundError("읽을 수 있는 CSV 파일이 없습니다.")
            
            # 모든 데이터프레임 병합
            df = dataframes[0]
            for temp_df in dataframes[1:]:
                df = df.union(temp_df)
            
            total_rows = df.count()
            logger.info(f"총 {len(csv_files)}개 파일에서 {total_rows}개 행 로드 완료")
            logger.info(f"컬럼명: {df.columns}")
            
            # 기업 필터링 (기관 컬럼에서 해당 기업이 포함된 행들을 가져옴)
            if '기관' in df.columns:
                # 기관 컬럼에 NaN이 아니고 회사명이 포함된 행 필터링
                filtered_df = df.filter(
                    (df['기관'].isNotNull()) & 
                    (df['기관'].contains(company_name))
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
                    keyword_rows = filtered_df.select("키워드").filter(df['키워드'].isNotNull()).collect()
                    
                    all_keywords = []
                    for row in keyword_rows:
                        keywords_str = row['키워드']
                        if keywords_str:
                            keywords = [re.sub(r'[^가-힣a-zA-Z0-9\s]', '', k.strip()) for k in keywords_str.split(',') if k.strip()]
                            all_keywords.extend([k for k in keywords if len(k) >= 2])
                    
                    # 기업명 자체는 키워드에서 제외
                    all_keywords = [k for k in all_keywords if company_name not in k]
                    
                    # 키워드 빈도 계산
                    keyword_counter = Counter(all_keywords)
                    
                    # 빈도순으로 정렬하여 딕셔너리 생성
                    keywords_dict = dict(keyword_counter.most_common())
                    
                    # 상위 키워드 추출
                    top_keywords_list = list(keywords_dict.keys())[:top_keywords]
                    
                    # 상위 키워드가 많이 포함된 뉴스 기사들 추출
                    top_news_articles = self.extract_top_news_articles(filtered_df, top_keywords_list)
                    
                    logger.info(f"🚀 PySpark 엔진으로 키워드 추출 완료: {len(keywords_dict)}개 키워드")
                    
                    return {
                        "company_name": company_name,
                        "period": f"{start_date}-{end_date}",
                        "total_news_count": total_count,
                        "keywords": keywords_dict,
                        "top_keywords": top_keywords_list,
                        "top_news_articles": top_news_articles,
                        "message": f"🚀 PySpark 엔진으로 성공적으로 키워드를 추출했습니다. 총 {len(keywords_dict)}개 키워드 발견 (파일 {len(csv_files)}개 처리)"
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
            raise e
    
    def extract_top_news_articles(self, filtered_df, top_keywords_list, max_articles=10):
        """
        상위 키워드가 많이 포함된 뉴스 기사들을 추출합니다.
        
        Args:
            filtered_df: 필터링된 Spark DataFrame
            top_keywords_list: 상위 키워드 리스트
            max_articles: 최대 기사 수
            
        Returns:
            List[Dict]: 뉴스 기사 정보 리스트
        """
        try:
            if not top_keywords_list:
                return []
            
            # 필요한 컬럼이 있는지 확인
            required_columns = ['제목', '날짜', 'URL', '키워드']
            available_columns = [col for col in required_columns if col in filtered_df.columns]
            
            if len(available_columns) < 3:  # 최소 3개 컬럼 필요
                logger.warning(f"필요한 컬럼이 부족합니다. 사용 가능: {available_columns}")
                return []
            
            # 각 기사에서 상위 키워드 매칭 개수 계산
            articles_with_score = []
            
            # DataFrame을 collect하여 Python에서 처리
            articles_data = filtered_df.select(*available_columns).collect()
            
            for row in articles_data:
                article_keywords = []
                if row.get('키워드') and str(row['키워드']).strip():
                    # 기사 키워드 분리
                    article_keywords = [k.strip() for k in str(row['키워드']).split(',') if k.strip()]
                
                # 상위 키워드와 매칭되는 개수 계산
                matched_count = 0
                matched_keywords = []
                for keyword in top_keywords_list:
                    for article_keyword in article_keywords:
                        if keyword in article_keyword or article_keyword in keyword:
                            matched_count += 1
                            matched_keywords.append(keyword)
                            break  # 중복 카운트 방지
                
                if matched_count > 0:
                    # nan 값 처리
                    title = row.get('제목', '제목 없음')
                    if title is None or str(title).lower() == 'nan':
                        title = '제목 없음'
                    
                    date = row.get('일자', '일자 없음')
                    if date is None or str(date).lower() == 'nan':
                        date = '일자 없음'
                    
                    url = row.get('URL', 'URL 없음')
                    if url is None or str(url).lower() == 'nan':
                        url = 'URL 없음'
                    
                    articles_with_score.append({
                        'title': str(title),
                        'date': str(date),
                        'url': str(url),
                        'matched_keywords_count': matched_count,
                        'matched_keywords': list(set(matched_keywords)),
                        'all_keywords': article_keywords
                    })
            
            # 매칭된 키워드 개수 순으로 정렬
            articles_with_score.sort(key=lambda x: x['matched_keywords_count'], reverse=True)
            
            # 상위 기사들만 반환
            top_articles = articles_with_score[:max_articles]
            
            logger.info(f"상위 키워드가 포함된 뉴스 기사 {len(top_articles)}개 추출 완료")
            
            return top_articles
            
        except Exception as e:
            logger.warning(f"뉴스 기사 추출 중 오류: {e}")
            return []
