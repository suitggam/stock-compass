#!/bin/bash

echo "=== 간소화된 Hadoop + Spark 환경 ==="

echo "제거된 불필요한 서비스들:"
echo "❌ Kafka + Zookeeper (메시지 큐 - 키워드 추출에 불필요)"
echo "❌ Spark Master/Worker (재시작 문제로 불안정)"
echo ""

echo "유지된 필수 서비스들:"
echo "✅ Hadoop NameNode (HDFS)"
echo "✅ Hadoop DataNode (HDFS)"
echo "✅ Hadoop ResourceManager (YARN)"
echo "✅ Hadoop NodeManager (YARN)"
echo "✅ Jupyter Notebook (Spark 실행 환경)"
echo ""

echo "=== 환경 시작 ==="
echo "1. 기존 컨테이너 중지 및 제거..."
docker-compose down

echo ""
echo "2. 간소화된 환경 시작..."
docker-compose up -d

echo ""
echo "3. 서비스 시작 대기 (30초)..."
sleep 30

echo ""
echo "4. HDFS 디렉토리 생성..."
docker exec namenode bash -c "hdfs dfs -mkdir -p /user/data/excel_uploads"
docker exec namenode bash -c "hdfs dfs -mkdir -p /user/data/results"
docker exec namenode bash -c "hdfs dfs -chmod 777 /user/data/excel_uploads"
docker exec namenode bash -c "hdfs dfs -chmod 777 /user/data/results"

echo ""
echo "5. 컨테이너 상태 확인..."
docker ps

echo ""
echo "=== 사용 가능한 서비스 ==="
echo "✅ Hadoop NameNode: http://localhost:9870"
echo "✅ Hadoop ResourceManager: http://localhost:8088"
echo "✅ Jupyter Notebook: http://localhost:8888"
echo ""

echo "=== 키워드 추출 방법 ==="
echo ""
echo "방법 1: Jupyter Notebook 사용 (권장)"
echo "1. http://localhost:8888 접속"
echo "2. notebooks/news_keyword_analysis.ipynb 파일 열기"
echo "3. 셀을 순서대로 실행"
echo ""
echo "방법 2: Jupyter 컨테이너에서 직접 실행"
echo "docker exec jupyter bash -c \"spark-submit --packages com.crealytics:spark-excel_2.12:3.3.1_0.18.5 /home/jovyan/work/spark-apps/news_keyword_extractor.py --input-path hdfs://namenode:9000/user/data/excel_uploads/NewsResult_20220901-20220905.xlsx --output-path hdfs://namenode:9000/user/data/results/keywords\""
echo ""
echo "방법 3: 파일 업로드 후 실행"
echo "./upload_to_hdfs_gitbash.sh \"news/NewsResult_20220901-20220905.xlsx\""
echo "docker exec jupyter bash -c \"spark-submit --packages com.crealytics:spark-excel_2.12:3.3.1_0.18.5 /home/jovyan/work/spark-apps/news_keyword_extractor.py --input-path hdfs://namenode:9000/user/data/excel_uploads/NewsResult_20220901-20220905.xlsx --output-path hdfs://namenode:9000/user/data/results/keywords\""
