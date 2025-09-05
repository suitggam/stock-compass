#!/bin/bash

# CSV 파일을 HDFS에 업로드하는 스크립트 (Git Bash용)

if [ $# -eq 0 ]; then
    echo "사용법: $0 <CSV파일경로>"
    echo "예시: $0 news/NewsResult_20220901-20220905.csv"
    exit 1
fi

CSV_FILE="$1"
FILE_NAME=$(basename "$CSV_FILE")

echo "=== CSV 파일 HDFS 업로드 (Git Bash) ==="
echo "업로드할 파일: $CSV_FILE"
echo "파일명: $FILE_NAME"

# Docker 컨테이너 상태 확인
echo "1. Docker 컨테이너 상태 확인 중..."
if ! docker ps | grep -q "namenode"; then
    echo "오류: namenode 컨테이너가 실행되지 않았습니다."
    echo "다음 명령어로 Docker를 시작하세요:"
    echo "docker-compose up -d"
    exit 1
fi

# 파일 존재 확인
if [ ! -f "$CSV_FILE" ]; then
    echo "오류: 파일 '$CSV_FILE'을 찾을 수 없습니다."
    exit 1
fi

# HDFS 디렉토리 생성
echo "2. HDFS 디렉토리 생성 중..."
docker exec namenode bash -c "hdfs dfs -mkdir -p /user/data/csv_uploads" 2>/dev/null || true

# 파일을 임시 컨테이너로 복사
echo "3. 파일을 임시 컨테이너로 복사 중..."
docker cp "$CSV_FILE" namenode:/tmp/

# 기존 파일이 있다면 삭제
echo "4. 기존 파일 확인 및 삭제 중..."
docker exec namenode bash -c "hdfs dfs -rm /user/data/csv_uploads/$FILE_NAME" 2>/dev/null || true

# 새 파일 업로드
echo "5. 새 파일 업로드 중..."
docker exec namenode bash -c "hdfs dfs -put /tmp/$FILE_NAME /user/data/csv_uploads/"

# 업로드 확인
echo "6. 업로드 확인 중..."
docker exec namenode bash -c "hdfs dfs -ls /user/data/csv_uploads/$FILE_NAME"

if [ $? -eq 0 ]; then
    echo "✅ CSV 파일 업로드 성공!"
    echo "HDFS 경로: hdfs://namenode:9000/user/data/csv_uploads/$FILE_NAME"
else
    echo "❌ CSV 파일 업로드 실패"
    exit 1
fi

# 임시 파일 정리
echo "7. 임시 파일 정리 중..."
docker exec namenode bash -c "rm -f /tmp/$FILE_NAME"

echo "업로드 완료!"
