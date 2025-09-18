#!/usr/bin/env python3
"""
뉴스 키워드 추출 FastAPI 애플리케이션
기간별 기업 키워드 추출 서비스를 제공합니다.
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from datetime import datetime
import logging
import os
from typing import Optional, Dict, List
from contextlib import asynccontextmanager
from keyword_extractor import KeywordExtractor

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class KeywordRequest(BaseModel):
    """키워드 추출 요청 모델"""
    company_name: str  # 기업명 (예: "삼성전자")
    start_date: str    # 시작 날짜 (YYYYMMDD 형식, 예: "20200901")
    end_date: str      # 종료 날짜 (YYYYMMDD 형식, 예: "20200903")
    top_keywords: Optional[int] = 20  # 상위 키워드 개수 (기본값: 20)
    use_ai_filter: Optional[bool] = True  # AI 필터링 사용 여부 (기본값: True)

class NewsArticle(BaseModel):
    """뉴스 기사 정보 모델"""
    title: str
    date: str
    url: str
    matched_keywords_count: int
    matched_keywords: List[str]

class KeywordResponse(BaseModel):
    """키워드 추출 응답 모델"""
    company_name: str
    period: str
    total_news_count: int
    keywords: Dict[str, int]  # {"키워드": 빈도수, ...}
    top_keywords: List[str]
    top_news_articles: Optional[List[NewsArticle]] = []  # 상위 키워드가 많이 포함된 뉴스 기사들
    message: str
    ai_filtered: Optional[bool] = False  # AI 필터링 적용 여부
    ai_analysis: Optional[str] = ""  # AI 분석 결과
    original_keyword_count: Optional[int] = 0  # 원본 키워드 개수
    filtered_keyword_count: Optional[int] = 0  # 필터링된 키워드 개수

# 키워드 추출기 인스턴스
keyword_extractor = KeywordExtractor()

@asynccontextmanager
async def lifespan(app: FastAPI):
    """앱 생명주기 관리"""
    # 시작 시
    logger.info("FastAPI 애플리케이션이 시작되었습니다.")
    yield
    # 종료 시
    keyword_extractor.cleanup()
    logger.info("FastAPI 애플리케이션이 종료되었습니다.")

app = FastAPI(
    title="뉴스 키워드 추출 API",
    description="기간별 기업의 키워드 추출을 위한 FastAPI 서비스",
    version="1.0.0",
    lifespan=lifespan
)

@app.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "message": "뉴스 키워드 추출 API에 오신 것을 환영합니다!",
        "version": "1.0.0",
        "endpoints": {
            "키워드 추출 (AI 필터링 포함)": "/extract-keywords",
            "API 문서": "/docs",
            "헬스체크": "/health"
        },
        "features": {
            "빈도수 기반 키워드 추출": "기존 키워드 추출 방식",
            "AI 스마트 필터링": "OpenAI를 활용한 주가 관련 키워드 필터링",
            "키워드 분석": "AI 기반 키워드 트렌드 분석"
        }
    }

@app.get("/health")
async def health_check():
    """헬스체크 엔드포인트"""
    return {"status": "healthy", "timestamp": datetime.now().isoformat()}

@app.post("/extract-keywords", response_model=KeywordResponse)
async def extract_keywords(request: KeywordRequest):
    """
    기업의 키워드를 추출하는 메인 엔드포인트 (AI 필터링 지원)
    
    Args:
        request: 키워드 추출 요청 (회사명, 시작일자, 종료일자, 상위 키워드 개수, AI 필터링 사용 여부)
    
    Returns:
        KeywordResponse: 추출된 키워드와 빈도수, AI 분석 결과
    
    Features:
        - 빈도수 기반 키워드 추출
        - OpenAI를 활용한 주가 관련 키워드 필터링
        - AI 기반 키워드 트렌드 분석
    
    Example:
        POST /extract-keywords
        {
            "company_name": "삼성전자",
            "start_date": "20200901", 
            "end_date": "20200903",
            "top_keywords": 20,
            "use_ai_filter": true
        }
    """
    try:
        logger.info(f"키워드 추출 요청: {request.company_name}, {request.start_date}-{request.end_date}")
        
        # 날짜 형식 검증
        try:
            datetime.strptime(request.start_date, "%Y%m%d")
            datetime.strptime(request.end_date, "%Y%m%d")
        except ValueError:
            raise HTTPException(status_code=400, detail="날짜 형식이 올바르지 않습니다. YYYYMMDD 형식을 사용해주세요.")
        
        # 키워드 추출 실행 (AI 필터링 옵션 포함)
        if request.use_ai_filter:
            result = keyword_extractor.extract_smart_keywords_from_csv(
                company_name=request.company_name,
                start_date=request.start_date,
                end_date=request.end_date,
                top_keywords=request.top_keywords,
                use_ai_filter=request.use_ai_filter
            )
        else:
            result = keyword_extractor.extract_keywords_from_csv(
                company_name=request.company_name,
                start_date=request.start_date,
                end_date=request.end_date,
                top_keywords=request.top_keywords
            )
        
        # 응답 형식에 맞게 변환 (상위 키워드만)
        top_keywords_dict = dict(list(result["keywords"].items())[:request.top_keywords])
        
        # 뉴스 기사 정보 변환
        top_news_articles = []
        if "top_news_articles" in result and result["top_news_articles"]:
            for article in result["top_news_articles"]:
                # nan 값 처리
                url = article.get("url", "URL 없음")
                if url is None or (isinstance(url, float) and str(url).lower() == 'nan'):
                    url = "URL 없음"
                
                title = article.get("title", "제목 없음")
                if title is None or (isinstance(title, float) and str(title).lower() == 'nan'):
                    title = "제목 없음"
                
                date = article.get("date", "날짜 없음")
                if date is None or (isinstance(date, float) and str(date).lower() == 'nan'):
                    date = "날짜 없음"
                
                top_news_articles.append(NewsArticle(
                    title=str(title),
                    date=str(date),
                    url=str(url),
                    matched_keywords_count=article.get("matched_keywords_count", 0),
                    matched_keywords=article.get("matched_keywords", [])
                ))
        
        response = KeywordResponse(
            company_name=result["company_name"],
            period=result["period"],
            total_news_count=result["total_news_count"],
            keywords=top_keywords_dict,
            top_keywords=result["top_keywords"],
            top_news_articles=top_news_articles,
            message=result["message"],
            ai_filtered=result.get("ai_filtered", False),
            ai_analysis=result.get("ai_analysis", ""),
            original_keyword_count=result.get("original_keyword_count", 0),
            filtered_keyword_count=result.get("filtered_keyword_count", 0)
        )
        
        logger.info(f"키워드 추출 완료: {result['total_news_count']}개 뉴스에서 {len(result['keywords'])}개 키워드 추출")
        return response
        
    except FileNotFoundError as e:
        logger.error(f"파일을 찾을 수 없습니다: {str(e)}")
        raise HTTPException(status_code=404, detail=str(e))
    except ValueError as e:
        logger.error(f"잘못된 요청: {str(e)}")
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"내부 서버 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"키워드 추출 중 오류가 발생했습니다: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    print("🚀 뉴스 키워드 추출 API 서버를 시작합니다...")
    print("📊 API 문서: http://localhost:8000/docs")
    print("💓 헬스체크: http://localhost:8000/health") 
    print("🤖 AI 스마트 필터링 지원")
    print("⏹️  Ctrl+C를 눌러 서버를 종료할 수 있습니다.")
    
    uvicorn.run(
        app, 
        host="0.0.0.0", 
        port=8000,
        log_level="info"
    )