#!/usr/bin/env python3
"""
뉴스 키워드 추출 FastAPI 애플리케이션
기간별 기업 키워드 추출 서비스를 제공합니다.
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from datetime import datetime
import logging
from typing import Optional, Dict, List
from keyword_extractor import KeywordExtractor
from time import time

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="뉴스 키워드 추출 API",
    description="기간별 기업의 키워드 추출을 위한 FastAPI 서비스",
    version="1.0.0"
)

class KeywordRequest(BaseModel):
    """키워드 추출 요청 모델"""
    company_name: str  # 기업명 (예: "삼성전자")
    start_date: str    # 시작 날짜 (YYYYMMDD 형식, 예: "20200901")
    end_date: str      # 종료 날짜 (YYYYMMDD 형식, 예: "20200903")
    top_keywords: Optional[int] = 20  # 상위 키워드 개수 (기본값: 20)

class KeywordResponse(BaseModel):
    """키워드 추출 응답 모델"""
    company_name: str
    period: str
    total_news_count: int
    keywords: Dict[str, int]  # {"키워드": 빈도수, ...}
    top_keywords: List[str]
    message: str


# 키워드 추출기 인스턴스
keyword_extractor = KeywordExtractor()

@app.on_event("startup")
async def startup_event():
    """앱 시작 시 초기화"""
    logger.info("FastAPI 애플리케이션이 시작되었습니다.")

@app.on_event("shutdown")
async def shutdown_event():
    """앱 종료 시 정리"""
    keyword_extractor.cleanup()
    logger.info("FastAPI 애플리케이션이 종료되었습니다.")

@app.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "message": "뉴스 키워드 추출 API에 오신 것을 환영합니다!",
        "version": "1.0.0",
        "endpoints": {
            "키워드 추출": "/extract-keywords",
            "API 문서": "/docs",
            "헬스체크": "/health"
        }
    }

@app.get("/health")
async def health_check():
    """헬스체크 엔드포인트"""
    return {"status": "healthy", "timestamp": datetime.now().isoformat()}

@app.post("/extract-keywords", response_model=KeywordResponse)
async def extract_keywords(request: KeywordRequest):
    """
    기업의 키워드를 추출하는 메인 엔드포인트
    
    Args:
        request: 키워드 추출 요청 (회사명, 시작일자, 종료일자, 상위 키워드 개수)
    
    Returns:
        KeywordResponse: 추출된 키워드와 빈도수
    
    Example:
        POST /extract-keywords
        {
            "company_name": "삼성전자",
            "start_date": "20200901",
            "end_date": "20200903",
            "top_keywords": 20
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
        
        # 키워드 추출 실행
        result = keyword_extractor.extract_keywords_from_csv(
            company_name=request.company_name,
            start_date=request.start_date,
            end_date=request.end_date,
            top_keywords=request.top_keywords
        )
        
        # 응답 형식에 맞게 변환 (상위 키워드만)
        top_keywords_dict = dict(list(result["keywords"].items())[:request.top_keywords])
        
        response = KeywordResponse(
            company_name=result["company_name"],
            period=result["period"],
            total_news_count=result["total_news_count"],
            keywords=top_keywords_dict,
            top_keywords=result["top_keywords"],
            message=result["message"]
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
    uvicorn.run(app, host="0.0.0.0", port=8000)
