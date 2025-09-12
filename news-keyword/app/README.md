# 뉴스 키워드 추출 FastAPI 서비스

기간별 기업의 키워드 추출을 위한 FastAPI 웹 서비스입니다.

## 🚀 주요 기능

- **기업별 키워드 추출**: 특정 기업과 관련된 뉴스에서 키워드를 추출합니다.
- **기간 필터링**: 시작일과 종료일을 지정하여 특정 기간의 데이터만 분석합니다.
- **PySpark 기반**: 대용량 데이터 처리를 위해 PySpark를 사용합니다.
- **RESTful API**: 표준 HTTP API로 쉽게 연동할 수 있습니다.

## 📋 API 명세

### 1. 키워드 추출 API

**엔드포인트**: `POST /extract-keywords`

**요청 형식**:
```json
{
    "company_name": "삼성전자",
    "start_date": "20200901",
    "end_date": "20200903",
    "top_keywords": 20
}
```

**응답 형식**:
```json
{
    "company_name": "삼성전자",
    "period": "20200901-20200903",
    "total_news_count": 15,
    "keywords": [
        {"keyword": "반도체", "frequency": 8},
        {"keyword": "투자", "frequency": 5},
        {"keyword": "기술", "frequency": 3}
    ],
    "top_keywords": ["반도체", "투자", "기술"],
    "message": "성공적으로 키워드를 추출했습니다."
}
```

### 2. 헬스체크 API

**엔드포인트**: `GET /health`

**응답 형식**:
```json
{
    "status": "healthy",
    "timestamp": "2024-01-15T14:30:25.123456"
}
```

## 🛠️ 설치 및 실행

### 로컬 환경에서 실행

1. **의존성 설치**:
```bash
pip install -r requirements.txt
```

2. **서버 실행**:
```bash
python run_api.py
```

3. **API 문서 확인**:
브라우저에서 `http://localhost:8000/docs` 접속

### Docker로 실행

1. **Docker 이미지 빌드**:
```bash
docker-compose build
```

2. **서비스 실행**:
```bash
docker-compose up -d
```

3. **서비스 상태 확인**:
```bash
docker-compose ps
```

## 🧪 테스트

### 자동 테스트 실행

```bash
python test_client.py
```

### 수동 테스트 (curl 사용)

1. **헬스체크**:
```bash
curl -X GET "http://localhost:8000/health"
```

2. **키워드 추출**:
```bash
curl -X POST "http://localhost:8000/extract-keywords" \
     -H "Content-Type: application/json" \
     -d '{
       "company_name": "삼성전자",
       "start_date": "20200901",
       "end_date": "20200903",
       "top_keywords": 10
     }'
```

## 📁 프로젝트 구조

```
app/
├── main.py              # FastAPI 메인 애플리케이션
├── run_api.py           # 서버 실행 스크립트
├── test_client.py       # API 테스트 클라이언트
├── requirements.txt     # Python 의존성
├── Dockerfile          # Docker 이미지 설정
├── docker-compose.yml   # Docker Compose 설정
└── README.md           # 이 파일
```

## 🔧 주요 구성 요소

### KeywordExtractor 클래스

- **initialize_spark()**: SparkSession 초기화
- **find_csv_file()**: 날짜 범위에 해당하는 CSV 파일 검색
- **extract_keywords_from_csv()**: CSV에서 키워드 추출 및 빈도 계산

### API 엔드포인트

- **POST /extract-keywords**: 메인 키워드 추출 기능
- **GET /**: API 정보 및 사용 가능한 엔드포인트 목록
- **GET /health**: 서비스 상태 확인

## 📊 데이터 처리 흐름

1. **요청 접수**: 클라이언트에서 기업명과 날짜 범위 전송
2. **파일 검색**: 해당 기간의 CSV 파일 찾기
3. **데이터 로드**: PySpark로 CSV 파일 읽기
4. **필터링**: 기관 컬럼에서 해당 기업이 포함된 행 추출
5. **키워드 추출**: 키워드 컬럼에서 키워드 분리 및 정리
6. **빈도 계산**: 각 키워드의 출현 빈도 계산
7. **결과 반환**: JSON 형식으로 결과 반환

## ⚠️ 주의사항

1. **CSV 파일 위치**: `../news/` 디렉토리에 CSV 파일이 있어야 합니다.
2. **메모리 사용량**: 대용량 CSV 파일 처리 시 충분한 메모리가 필요합니다.
3. **날짜 형식**: YYYYMMDD 형식만 지원합니다 (예: 20200901).
4. **Java 환경**: PySpark 실행을 위해 Java 8 이상이 필요합니다.

## 🚀 확장 계획

- [ ] 여러 CSV 파일을 자동으로 찾아서 처리
- [ ] 캐싱 기능 추가로 응답 속도 개선
- [ ] 키워드 추출 알고리즘 개선 (TF-IDF, Word2Vec 등)
- [ ] 비동기 처리로 대용량 데이터 처리 성능 향상
- [ ] 인증 및 권한 관리 기능
- [ ] 로그 및 모니터링 기능 강화


