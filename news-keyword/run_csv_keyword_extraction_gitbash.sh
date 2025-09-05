#!/bin/bash

# CSV 파일 기반 키워드 추출 실행 스크립트 (Git Bash용)

if [ $# -eq 0 ]; then
    echo "사용법: $0 <CSV파일명> [옵션]"
    echo "예시: $0 NewsResult_20220901-20220905.csv"
    echo "옵션:"
    echo "  --company-filter '기업1,기업2'  : 특정 기업만 분석"
    echo "  --start-date 20220901          : 시작 날짜 (YYYYMMDD)"
    echo "  --end-date 20220905            : 종료 날짜 (YYYYMMDD)"
    echo "  --use-existing-keywords         : 기존 키워드 컬럼 사용"
    echo "  --top-keywords 20               : 상위 키워드 개수"
    exit 1
fi

FILE_NAME="$1"
shift  # 첫 번째 인수 제거하여 나머지 옵션들만 남김
OPTIONS="$@"

echo "=== CSV 기반 키워드 추출 실행 (Git Bash) ==="
echo "실행할 파일: $FILE_NAME"
echo "추가 옵션: $OPTIONS"

# 실행 시작 시간 기록
START_TIME=$(date +%s)
echo "실행 시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"

# Docker 컨테이너 상태 확인
echo "1. Docker 컨테이너 상태 확인 중..."
if ! docker ps | grep -q "jupyter"; then
    echo "오류: jupyter 컨테이너가 실행되지 않았습니다."
    echo "다음 명령어로 Docker를 시작하세요:"
    echo "docker-compose up -d"
    exit 1
fi

# HDFS 파일 존재 확인
echo "2. HDFS 파일 존재 확인 중..."
docker exec namenode bash -c "hdfs dfs -ls /user/data/csv_uploads/$FILE_NAME" >/dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "오류: HDFS에 파일이 없습니다. 먼저 업로드하세요:"
    echo "./upload_csv_to_hdfs_gitbash.sh news/$FILE_NAME"
    exit 1
fi

# 결과 디렉토리 생성
echo "3. 결과 디렉토리 생성 중..."
docker exec namenode bash -c "hdfs dfs -mkdir -p /user/data/results/csv_keywords" 2>/dev/null || true

# Spark 애플리케이션 실행
echo "4. Spark 애플리케이션 실행 중..."
INPUT_PATH="hdfs://namenode:9000/user/data/csv_uploads/$FILE_NAME"
OUTPUT_PATH="hdfs://namenode:9000/user/data/results/csv_keywords"

echo "명령어: spark-submit /home/jovyan/work/spark-apps/news_keyword_extractor.py --input-path $INPUT_PATH --output-path $OUTPUT_PATH $OPTIONS"

docker exec jupyter bash -c "spark-submit /home/jovyan/work/spark-apps/news_keyword_extractor.py --input-path $INPUT_PATH --output-path $OUTPUT_PATH $OPTIONS"

if [ $? -eq 0 ]; then
    # 실행 완료 시간 기록 및 계산
    END_TIME=$(date +%s)
    EXECUTION_TIME=$((END_TIME - START_TIME))
    
    echo "✅ 키워드 추출 성공!"
    echo ""
    echo "=== 실행 시간 정보 ==="
    echo "시작 시간: $(date -d @$START_TIME '+%Y-%m-%d %H:%M:%S')"
    echo "완료 시간: $(date -d @$END_TIME '+%Y-%m-%d %H:%M:%S')"
    echo "총 실행 시간: ${EXECUTION_TIME}초 ($(($EXECUTION_TIME / 60))분 $(($EXECUTION_TIME % 60))초)"
    echo ""
    echo "=== 결과 확인 방법 ==="
    echo "1. 기간별 분석 결과:"
    echo "   docker exec namenode bash -c \"hdfs dfs -ls $OUTPUT_PATH/period_analysis\""
    echo ""
    echo "2. 기업별 요약 결과:"
    echo "   docker exec namenode bash -c \"hdfs dfs -ls $OUTPUT_PATH/company_summary\""
    echo ""
    echo "3. 키워드 빈도 상세 결과:"
    echo "   docker exec namenode bash -c \"hdfs dfs -ls $OUTPUT_PATH/keyword_frequency\""
    echo ""
    echo "4. 결과 파일 내용 확인:"
    echo "   docker exec namenode bash -c \"hdfs dfs -cat $OUTPUT_PATH/company_summary/*.csv | head -10\""
else
    # 실패 시에도 실행 시간 출력
    END_TIME=$(date +%s)
    EXECUTION_TIME=$((END_TIME - START_TIME))
    
    echo "❌ 키워드 추출 실패"
    echo ""
    echo "=== 실행 시간 정보 ==="
    echo "시작 시간: $(date -d @$START_TIME '+%Y-%m-%d %H:%M:%S')"
    echo "실패 시간: $(date -d @$END_TIME '+%Y-%m-%d %H:%M:%S')"
    echo "실행 시간: ${EXECUTION_TIME}초 ($(($EXECUTION_TIME / 60))분 $(($EXECUTION_TIME % 60))초)"
    exit 1
fi
