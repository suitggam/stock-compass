#!/bin/bash

echo "=== 뉴스 파일 HDFS 업로드 (Git Bash) ==="

if [ $# -eq 0 ]; then
    echo "사용법: $0 \"파일경로\""
    echo "예시: $0 \"news/NewsResult_20220901-20220905.xlsx\""
    exit 1
fi

FILE_PATH="$1"
FILE_NAME=$(basename "$FILE_PATH")

echo "업로드할 파일: $FILE_PATH"
echo "파일명: $FILE_NAME"

# 파일 존재 확인
if [ ! -f "$FILE_PATH" ]; then
    echo "오류: 파일을 찾을 수 없습니다 - $FILE_PATH"
    exit 1
fi

echo ""
echo "1. 파일을 임시 컨테이너로 복사 중..."
docker cp "$FILE_PATH" namenode:/tmp/"$FILE_NAME"

if [ $? -ne 0 ]; then
    echo "오류: 파일 복사 실패"
    exit 1
fi

echo "2. HDFS에 파일 업로드 중..."
# 기존 파일이 있다면 삭제
echo "   기존 파일 확인 및 삭제 중..."
docker exec namenode bash -c "hdfs dfs -rm /user/data/excel_uploads/$FILE_NAME" 2>/dev/null || true

# 새 파일 업로드
echo "   새 파일 업로드 중..."
docker exec namenode bash -c "hdfs dfs -put /tmp/$FILE_NAME /user/data/excel_uploads/"

if [ $? -ne 0 ]; then
    echo "오류: HDFS 업로드 실패"
    exit 1
fi

echo "3. 임시 파일 정리 중..."
docker exec namenode rm /tmp/"$FILE_NAME"

echo "4. 업로드 완료!"
echo ""
echo "업로드된 파일 확인:"
docker exec namenode bash -c "hdfs dfs -ls /user/data/excel_uploads/"

echo ""
echo "이제 다음 명령어로 키워드 추출을 실행할 수 있습니다:"
echo "./run_keyword_extraction_gitbash.sh \"$FILE_NAME\""
