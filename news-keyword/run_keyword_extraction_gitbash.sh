#!/bin/bash

echo "=== 뉴스 키워드 추출 실행 (Git Bash) ==="

if [ $# -eq 0 ]; then
    echo "사용법: $0 \"파일명\" [옵션]"
    echo ""
    echo "옵션:"
    echo "  --company-filter \"기업명1,기업명2\"  : 특정 기업만 필터링"
    echo "  --start-date \"YYYY-MM-DD\"          : 시작 날짜"
    echo "  --end-date \"YYYY-MM-DD\"            : 종료 날짜"
    echo "  --min-keyword-length 숫자          : 최소 키워드 길이 (기본값: 2)"
    echo "  --top-keywords 숫자                : 상위 키워드 개수 (기본값: 10)"
    echo ""
    echo "예시: $0 \"news.xlsx\" --company-filter \"삼성전자,SK하이닉스\""
    exit 1
fi

FILE_NAME="$1"
shift
ADDITIONAL_ARGS="$@"

echo "실행할 파일: $FILE_NAME"
echo "추가 옵션: $ADDITIONAL_ARGS"

# HDFS에 파일이 존재하는지 확인
docker exec namenode bash -c "hdfs dfs -test -e /user/data/excel_uploads/$FILE_NAME"
if [ $? -ne 0 ]; then
    echo "오류: HDFS에 파일이 없습니다 - /user/data/excel_uploads/$FILE_NAME"
    echo "먼저 upload_to_hdfs_gitbash.sh을 사용하여 파일을 업로드하세요."
    exit 1
fi

echo ""
echo "Spark 애플리케이션 실행 중..."
echo "명령어: spark-submit --packages com.crealytics:spark-excel_2.12:3.3.1_0.18.5 /spark/apps/news_keyword_extractor.py --input-path hdfs://namenode:9000/user/data/excel_uploads/$FILE_NAME --output-path hdfs://namenode:9000/user/data/results/keywords $ADDITIONAL_ARGS"

docker exec spark-master spark-submit --packages com.crealytics:spark-excel_2.12:3.3.1_0.18.5 /spark/apps/news_keyword_extractor.py --input-path hdfs://namenode:9000/user/data/excel_uploads/"$FILE_NAME" --output-path hdfs://namenode:9000/user/data/results/keywords $ADDITIONAL_ARGS

if [ $? -ne 0 ]; then
    echo "오류: 키워드 추출 실패"
    exit 1
fi

echo ""
echo "키워드 추출 완료!"
echo ""
echo "결과 확인:"
docker exec namenode bash -c "hdfs dfs -ls /user/data/results/keywords/"
echo ""
echo "결과 내용 미리보기:"
docker exec namenode bash -c "hdfs dfs -cat /user/data/results/keywords/part-00000-*.csv"
