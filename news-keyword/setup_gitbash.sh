#!/bin/bash

echo "=== 뉴스 키워드 추출 환경 설정 (Git Bash) ==="

# Docker Compose로 서비스 시작
echo "1. Docker 서비스 시작 중..."
docker-compose up -d

# 서비스가 완전히 시작될 때까지 대기
echo "2. 서비스 시작 대기 중..."
sleep 30

# HDFS 디렉토리 생성
echo "3. HDFS 디렉토리 생성 중..."
docker exec namenode bash -c "hdfs dfs -mkdir -p /user/data/excel_uploads"
docker exec namenode bash -c "hdfs dfs -mkdir -p /user/data/results"
docker exec namenode bash -c "hdfs dfs -mkdir -p /spark-logs"

# 권한 설정
docker exec namenode bash -c "hdfs dfs -chmod 777 /user/data/excel_uploads"
docker exec namenode bash -c "hdfs dfs -chmod 777 /user/data/results"
docker exec namenode bash -c "hdfs dfs -chmod 777 /spark-logs"

# HDFS 상태 확인
echo "4. HDFS 상태 확인 중..."
docker exec namenode bash -c "hdfs dfs -ls -R /user/data"

echo "5. 환경 설정 완료!"
echo ""
echo "=== 접속 정보 ==="
echo "Hadoop NameNode Web UI: http://localhost:9870"
echo "Hadoop ResourceManager: http://localhost:8088"
echo "Spark Master Web UI: http://localhost:8080"
echo "Spark Worker 1: http://localhost:8081"
echo "Spark Worker 2: http://localhost:8082"
echo "Jupyter Notebook: http://localhost:8888"
echo "Kafka: localhost:9092"
echo ""
echo "=== 사용 방법 ==="
echo "1. 뉴스 엑셀 파일을 HDFS에 업로드:"
echo "   ./upload_to_hdfs_gitbash.sh \"news/NewsResult_20220901-20220905.xlsx\""
echo ""
echo "2. Spark 애플리케이션 실행:"
echo "   ./run_keyword_extraction_gitbash.sh \"NewsResult_20220901-20220905.xlsx\""
echo ""
echo "3. 결과 확인:"
echo "   docker exec namenode hdfs dfs -cat /user/data/results/keywords/part-00000-*.csv"
