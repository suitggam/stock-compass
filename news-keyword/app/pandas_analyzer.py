"""
Pandas를 사용한 키워드 추출 엔진
중소용량 데이터 처리에 최적화
"""

import os
import re
from typing import Dict, List
from collections import Counter
import logging
import pandas as pd

logger = logging.getLogger(__name__)

class PandasAnalyzer:
    """Pandas를 사용한 키워드 추출 분석기"""
    
    def __init__(self):
        pass
    
    def extract_keywords_with_pandas(self, company_name: str, start_date: str, end_date: str, top_keywords: int, csv_files: List[str]) -> Dict:
        """
        pandas를 사용한 키워드 추출 (백업 방법)
        여러 CSV 파일을 읽어서 통합 처리
        """
        logger.info("🐼 Pandas 엔진으로 키워드 추출을 시작합니다.")
        
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
                # 키워드 컬럼에서 키워드 분리 및 정리
                all_keywords = []
                for keywords_str in filtered_df['키워드'].dropna():
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
                
                return {
                    "company_name": company_name,
                    "period": f"{start_date}-{end_date}",
                    "total_news_count": total_count,
                    "keywords": keywords_dict,
                    "top_keywords": top_keywords_list,
                    "top_news_articles": top_news_articles,
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
    
    def extract_top_news_articles(self, filtered_df, top_keywords_list, max_articles=10):
        """
        상위 키워드가 많이 포함된 뉴스 기사들을 추출합니다.
        
        Args:
            filtered_df: 필터링된 데이터프레임
            top_keywords_list: 상위 키워드 리스트
            max_articles: 최대 기사 수
            
        Returns:
            List[Dict]: 뉴스 기사 정보 리스트
        """
        try:
            if not top_keywords_list:
                return []
            
            # 각 기사에서 상위 키워드 매칭 개수 계산
            articles_with_score = []
            
            for idx, row in filtered_df.iterrows():
                article_keywords = []
                if pd.notna(row.get('키워드', '')):
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
                    if pd.isna(title):
                        title = '제목 없음'
                    
                    date = row.get('일자', '일자 없음')
                    if pd.isna(date):
                        date = '일자 없음'
                    
                    url = row.get('URL', 'URL 없음')
                    if pd.isna(url):
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
