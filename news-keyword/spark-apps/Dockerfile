FROM bde2020/spark-worker:3.3.0-hadoop3.3

# 한국어 지원을 위한 패키지 설치
RUN apt-get update && apt-get install -y \
    openjdk-8-jdk \
    python3-pip \
    python3-dev \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Python 패키지 설치
COPY requirements.txt /tmp/
RUN pip3 install --no-cache-dir -r /tmp/requirements.txt

# 한국어 형태소 분석을 위한 Java 패키지 설치
RUN apt-get update && apt-get install -y \
    openjdk-8-jdk \
    && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /spark/apps

# 애플리케이션 파일 복사
COPY . /spark/apps/

# 실행 권한 부여
RUN chmod +x /spark/apps/*.py

# 환경 변수 설정
ENV PYTHONPATH=/spark/python:/spark/python/lib/py4j-0.10.9.5-src.zip:$PYTHONPATH
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# 기본 명령어
CMD ["/bin/bash"]
