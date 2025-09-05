# 뉴스 키워드 추출 분석 시스템

Hadoop과 Spark를 사용하여 뉴스 데이터에서 기간별 기업 키워드를 추출하는 시스템입니다.

## 📁 프로젝트 구조

```
news-keyword/
├── docker-compose.yml              # Docker 환경 구성
├── hadoop-config/                  # Hadoop 설정 파일
│   ├── core-site.xml              # Hadoop 핵심 설정
│   ├── hdfs-site.xml              # HDFS 분산 파일시스템 설정
│   ├── yarn-site.xml              # YARN 리소스 관리자 설정
│   └── hadoop.env                 # 환경 변수 설정
├── spark-apps/                     # Spark 애플리케이션
│   ├── news_keyword_extractor.py  # 메인 키워드 추출 애플리케이션
│   ├── requirements.txt           # Python 의존성
│   └── Dockerfile                 # Spark 앱용 Dockerfile
├── news/                          # 원본 뉴스 데이터
│   └── NewsResult_20220901-20220905.csv
├── scripts/                       # 실행 스크립트
│   ├── setup_gitbash.sh           # HDFS 초기 설정
│   ├── upload_csv_to_hdfs_gitbash.sh  # CSV 파일 업로드
│   └── run_csv_keyword_extraction_gitbash.sh  # 키워드 추출 실행
└── README.md                      # 이 파일
```

## 🚀 빠른 시작

### 1. Docker 환경 시작
```bash
# Docker Compose 시작
docker-compose up -d

# 컨테이너 상태 확인
docker ps
```

### 2. HDFS 초기 설정
```bash
# HDFS 디렉토리 생성 및 권한 설정
./setup_gitbash.sh
```

### 3. CSV 파일 업로드
```bash
# CSV 파일을 HDFS에 업로드
./upload_csv_to_hdfs_gitbash.sh "news/NewsResult_20220901-20220905.csv"
```

### 4. 키워드 추출 실행
```bash
# 기본 실행 (전체 데이터 분석)
./run_csv_keyword_extraction_gitbash.sh "NewsResult_20220901-20220905.csv"

# 기존 키워드 컬럼 사용 (권장)
./run_csv_keyword_extraction_gitbash.sh "NewsResult_20220901-20220905.csv" --use-existing-keywords

# 특정 기업만 분석
./run_csv_keyword_extraction_gitbash.sh "NewsResult_20220901-20220905.csv" --use-existing-keywords --company-filter "농협"

# 특정 기간만 분석
./run_csv_keyword_extraction_gitbash.sh "NewsResult_20220901-20220905.csv" --use-existing-keywords --start-date 20220901 --end-date 20220903
```

## 📊 데이터 구조

### 입력 CSV 파일 컬럼
- **뉴스 식별자**: 고유 ID
- **일자**: 날짜 (YYYYMMDD 형식)
- **언론사**: 뉴스 출처
- **제목**: 뉴스 제목
- **기관**: 관련 기업/기관명
- **키워드**: 이미 추출된 키워드들
- **특성추출(가중치순 상위 50개)**: 가중치가 적용된 상위 키워드들
- **본문**: 뉴스 본문

### 출력 결과 파일

#### 1. `period_analysis/` - 기간별 분석 결과
```csv
date_formatted,company,top_keywords_str,top_frequencies_str
2022-09-05,농협,"농협,대전,지역,본부,추석,명절,대비,식품,안전,특별,점검...","1,1,1,1,1,1,1,1,1,1,1..."
```

#### 2. `company_summary/` - 기업별 요약 결과
```csv
company,top_keywords_str,top_frequencies_str
농협,"농협,대전,지역,본부,추석,명절,대비,식품,안전,특별,점검...","1,1,1,1,1,1,1,1,1,1,1..."
```

#### 3. `keyword_frequency/` - 키워드 빈도 상세 데이터
```csv
date_formatted,company,keyword,frequency
2022-09-05,농협,농협,1
2022-09-05,농협,대전,1
2022-09-05,농협,지역,1
```

## 🔧 주요 기능

### 키워드 추출 방법
1. **기존 키워드 컬럼 사용** (`--use-existing-keywords`)
   - CSV 파일에 이미 추출된 키워드 활용
   - 더 정확하고 빠른 분석
   - 권장 방법

2. **제목/본문에서 새로 추출** (기본값)
   - 원본 텍스트에서 단어 분리
   - 커스터마이징 가능

### 필터링 옵션
- **기간 필터**: `--start-date`, `--end-date` (YYYYMMDD 형식)
- **기업 필터**: `--company-filter` (쉼표로 구분)
- **키워드 개수**: `--top-keywords` (기본값: 20)

## 📈 실행 시간 측정

스크립트 실행 시 자동으로 실행 시간을 측정하고 출력합니다:

```
=== 실행 시간 정보 ===
시작 시간: 2024-01-15 14:30:25
완료 시간: 2024-01-15 14:32:18
총 실행 시간: 113초 (1분 53초)
```

### 예상 실행 시간
- **소규모 데이터** (1-2만 행): 1-3분
- **중간 규모 데이터** (5-10만 행): 3-10분
- **대규모 데이터** (10만 행 이상): 10-30분

## 🔍 결과 확인

### HDFS에서 결과 확인
```bash
# 기간별 분석 결과
docker exec namenode bash -c "hdfs dfs -ls /user/data/results/csv_keywords/period_analysis"

# 기업별 요약 결과
docker exec namenode bash -c "hdfs dfs -ls /user/data/results/csv_keywords/company_summary"

# 키워드 빈도 상세 결과
docker exec namenode bash -c "hdfs dfs -ls /user/data/results/csv_keywords/keyword_frequency"
```

### 결과 내용 확인
```bash
# 기업별 상위 키워드 확인
docker exec namenode bash -c "hdfs dfs -cat /user/data/results/csv_keywords/company_summary/*.csv | head -20"

# 기간별 키워드 확인
docker exec namenode bash -c "hdfs dfs -cat /user/data/results/csv_keywords/period_analysis/*.csv | head -20"
```

### 결과 다운로드
```bash
# 결과를 로컬로 다운로드
docker exec namenode bash -c "hdfs dfs -get /user/data/results/csv_keywords/company_summary /tmp/"
docker cp namenode:/tmp/company_summary ./results/
```

## 🛠️ 기술 스택

- **Hadoop**: HDFS (분산 파일시스템), YARN (리소스 관리)
- **Spark**: 분산 데이터 처리 엔진
- **Docker**: 컨테이너화된 환경
- **Python**: PySpark 애플리케이션
- **CSV**: 데이터 형식

## 💻 코드 구조 설명

### 1. 메인 Spark 애플리케이션 (`spark-apps/news_keyword_extractor.py`)

#### 주요 함수들:
```python
def clean_text(text):
    """텍스트 전처리 함수"""
    # HTML 태그 제거, 특수문자 제거, 공백 정리

def extract_keywords(text, min_length=2):
    """키워드 추출 함수"""
    # 단어 분리 및 길이 필터링

def main():
    """메인 실행 함수"""
    # 1. 명령행 인수 파싱
    # 2. SparkSession 생성
    # 3. CSV 파일 읽기
    # 4. 날짜/기업 필터링
    # 5. 키워드 추출 (기존 키워드 또는 새로 추출)
    # 6. 기간별/기업별 분석
    # 7. 결과 저장
```

#### 핵심 로직 상세 해설:

##### 1. CSV 파일 읽기 및 스키마 추론
```python
df = spark.read.option("header", "true").csv(args.input_path)
```
**해설**: 
- `header="true"`: 첫 번째 행을 컬럼명으로 사용
- `inferSchema="true"`: 자동으로 데이터 타입 추론
- HDFS에서 직접 CSV 파일을 읽어 DataFrame 생성

##### 2. 날짜 필터링 로직
```python
if '일자' in df.columns:
    # YYYYMMDD 형식을 YYYY-MM-DD로 변환
    df = df.withColumn('date_formatted', 
        date_format(to_date(col('일자'), 'yyyyMMdd'), 'yyyy-MM-dd'))
    
    if args.start_date:
        start_date_formatted = f"{args.start_date[:4]}-{args.start_date[4:6]}-{args.start_date[6:8]}"
        df = df.filter(col('date_formatted') >= start_date_formatted)
```
**해설**:
- `to_date(col('일자'), 'yyyyMMdd')`: 문자열을 날짜 타입으로 변환
- `date_format(..., 'yyyy-MM-dd')`: 날짜를 표준 형식으로 변환
- `filter(col('date_formatted') >= start_date_formatted)`: 날짜 범위 필터링

##### 3. 기업 필터링 로직 (부분 매칭 지원)
```python
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
```
**해설**:
- `args.company_filter.split(',')`: 쉼표로 구분된 기업명을 리스트로 분리
- `[c.strip() for c in ...]`: 각 기업명의 앞뒤 공백 제거
- `col('기관').contains(company)`: 기관 컬럼에 해당 기업이 포함되는지 확인 (부분 매칭)
- `filter_condition | col('기관').contains(company)`: 여러 기업 중 하나라도 포함되면 매칭 (OR 조건)
- **예시**: 지정 기업 "농협"이 기관 "농협,농협대전지역본부,대전농협"에 포함되어 매칭됨

##### 4. 키워드 추출 로직 (기존 키워드 사용)
```python
if args.use_existing_keywords and '키워드' in df.columns:
    keywords_df = df.select(
        col('일자').alias('date'),
        col('기관').alias('company'),
        explode(split(col('키워드'), ',')).alias('keyword')
    ).filter(col('keyword') != '')
```
**해설**:
- `split(col('키워드'), ',')`: 키워드 컬럼을 쉼표로 분리하여 배열 생성
- `explode(...)`: 배열의 각 요소를 개별 행으로 변환
- `filter(col('keyword') != '')`: 빈 키워드 제거

##### 5. 키워드 빈도 계산 로직
```python
keyword_freq = keywords_df.groupBy('date_formatted', 'company', 'keyword') \
    .agg(count('*').alias('frequency')) \
    .orderBy(col('date_formatted'), col('company'), col('frequency').desc())
```
**해설**:
- `groupBy('date_formatted', 'company', 'keyword')`: 날짜, 기업, 키워드별로 그룹화
- `count('*').alias('frequency')`: 각 그룹의 행 개수를 빈도로 계산
- `orderBy(..., col('frequency').desc())`: 빈도 내림차순으로 정렬

##### 6. 상위 키워드 추출 로직
```python
top_keywords_by_period = keyword_freq.groupBy('date_formatted', 'company') \
    .agg(collect_list('keyword').alias('keywords'),
         collect_list('frequency').alias('frequencies')) \
    .withColumn('top_keywords', 
        when(size(col('keywords')) > args.top_keywords, 
             slice(col('keywords'), 1, args.top_keywords))
        .otherwise(col('keywords')))
```
**해설**:
- `collect_list('keyword')`: 각 그룹의 키워드들을 리스트로 수집
- `when(size(col('keywords')) > args.top_keywords, ...)`: 키워드 개수가 제한보다 많으면
- `slice(col('keywords'), 1, args.top_keywords)`: 상위 N개만 선택
- `otherwise(col('keywords'))`: 그렇지 않으면 전체 사용

##### 7. 결과 저장 로직 (문자열 변환)
```python
.withColumn('top_keywords_str', 
    regexp_replace(col('top_keywords').cast('string'), r'[\[\]]', ''))
```
**해설**:
- `col('top_keywords').cast('string')`: 배열을 문자열로 변환
- `regexp_replace(..., r'[\[\]]', '')`: 대괄호 `[`, `]` 제거
- CSV 형식에 맞게 배열을 쉼표로 구분된 문자열로 변환

### 2. 실행 스크립트들

#### `setup_gitbash.sh` - HDFS 초기 설정 핵심 로직
```bash
#!/bin/bash
# 1. Docker 컨테이너 상태 확인
if ! docker ps | grep -q "namenode"; then
    echo "오류: namenode 컨테이너가 실행되지 않았습니다."
    exit 1
fi

# 2. HDFS 디렉토리 생성
docker exec namenode bash -c "hdfs dfs -mkdir -p /user/data/excel_uploads"
docker exec namenode bash -c "hdfs dfs -mkdir -p /user/data/csv_uploads"

# 3. 권한 설정
docker exec namenode bash -c "hdfs dfs -chmod 777 /user/data"
```
**핵심 로직 해설**:
- `docker ps | grep -q "namenode"`: namenode 컨테이너 실행 상태 확인
- `hdfs dfs -mkdir -p`: HDFS에 디렉토리 생성 (`-p`: 상위 디렉토리도 함께 생성)
- `hdfs dfs -chmod 777`: 모든 사용자에게 읽기/쓰기/실행 권한 부여
- `bash -c "..."`: Docker 컨테이너 내부에서 명령어 실행

#### `upload_csv_to_hdfs_gitbash.sh` - CSV 파일 업로드 핵심 로직
```bash
#!/bin/bash
# 1. 파일 존재 확인
if [ ! -f "$CSV_FILE" ]; then
    echo "오류: 파일 '$CSV_FILE'을 찾을 수 없습니다."
    exit 1
fi

# 2. Docker 컨테이너로 파일 복사
docker cp "$CSV_FILE" namenode:/tmp/

# 3. 기존 파일 삭제 (중복 방지)
docker exec namenode bash -c "hdfs dfs -rm /user/data/csv_uploads/$FILE_NAME" 2>/dev/null || true

# 4. HDFS에 새 파일 업로드
docker exec namenode bash -c "hdfs dfs -put /tmp/$FILE_NAME /user/data/csv_uploads/"

# 5. 업로드 확인
docker exec namenode bash -c "hdfs dfs -ls /user/data/csv_uploads/$FILE_NAME"
```
**핵심 로직 해설**:
- `[ ! -f "$CSV_FILE" ]`: 파일 존재 여부 확인
- `docker cp "$CSV_FILE" namenode:/tmp/`: 로컬 파일을 Docker 컨테이너로 복사
- `hdfs dfs -rm ... 2>/dev/null || true`: 기존 파일 삭제 (오류 무시)
- `hdfs dfs -put`: 로컬 파일을 HDFS에 업로드
- `hdfs dfs -ls`: 업로드된 파일 확인

#### `run_csv_keyword_extraction_gitbash.sh` - 키워드 추출 실행 핵심 로직
```bash
#!/bin/bash
# 1. 실행 시간 측정 시작
START_TIME=$(date +%s)
echo "실행 시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"

# 2. Docker 컨테이너 상태 확인
if ! docker ps | grep -q "jupyter"; then
    echo "오류: jupyter 컨테이너가 실행되지 않았습니다."
    exit 1
fi

# 3. HDFS 파일 존재 확인
docker exec namenode bash -c "hdfs dfs -ls /user/data/csv_uploads/$FILE_NAME" >/dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "오류: HDFS에 파일이 없습니다."
    exit 1
fi

# 4. Spark 애플리케이션 실행
INPUT_PATH="hdfs://namenode:9000/user/data/csv_uploads/$FILE_NAME"
OUTPUT_PATH="hdfs://namenode:9000/user/data/results/csv_keywords"

docker exec jupyter bash -c "spark-submit /home/jovyan/work/spark-apps/news_keyword_extractor.py --input-path $INPUT_PATH --output-path $OUTPUT_PATH $OPTIONS"

# 5. 실행 시간 계산 및 출력
if [ $? -eq 0 ]; then
    END_TIME=$(date +%s)
    EXECUTION_TIME=$((END_TIME - START_TIME))
    echo "총 실행 시간: ${EXECUTION_TIME}초 ($(($EXECUTION_TIME / 60))분 $(($EXECUTION_TIME % 60))초)"
else
    echo "❌ 키워드 추출 실패"
    exit 1
fi
```
**핵심 로직 해설**:
- `START_TIME=$(date +%s)`: Unix 타임스탬프로 시작 시간 기록
- `docker ps | grep -q "jupyter"`: jupyter 컨테이너 실행 상태 확인
- `hdfs dfs -ls ... >/dev/null 2>&1`: HDFS 파일 존재 확인 (출력 숨김)
- `$? -ne 0`: 이전 명령어의 종료 코드 확인 (0이 아니면 실패)
- `spark-submit`: Spark 애플리케이션 실행
- `EXECUTION_TIME=$((END_TIME - START_TIME))`: 실행 시간 계산
- `$(($EXECUTION_TIME / 60))`: 분 단위 계산
- `$(($EXECUTION_TIME % 60))`: 초 단위 계산

## 📋 사용 예시

### 기본 분석
```bash
./run_csv_keyword_extraction_gitbash.sh "NewsResult_20220901-20220905.csv" --use-existing-keywords
```

### 특정 기업 분석
```bash
./run_csv_keyword_extraction_gitbash.sh "NewsResult_20220901-20220905.csv" --use-existing-keywords --company-filter "농협"
```

### 특정 기간 + 기업 분석
```bash
./run_csv_keyword_extraction_gitbash.sh "NewsResult_20220901-20220905.csv" --use-existing-keywords --start-date 20220901 --end-date 20220903 --company-filter "농협,대전교통공사"
```

### 여러 기업 동시 분석
```bash
./run_csv_keyword_extraction_gitbash.sh "NewsResult_20220901-20220905.csv" --use-existing-keywords --company-filter "농협,대전교통공사,대전시"
```

## ⚠️ 주의사항

1. **Git Bash 사용**: PowerShell이 아닌 Git Bash에서 실행
2. **파일 경로**: CSV 파일은 `news/` 디렉토리에 위치
3. **메모리**: 대용량 파일의 경우 충분한 메모리 필요
4. **Docker 상태**: 실행 전 Docker 컨테이너가 정상 동작하는지 확인
