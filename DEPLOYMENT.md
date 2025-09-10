# 🚀 Stock Trading System 배포 가이드

## 📋 개요
Spring Boot 백엔드와 React 프론트엔드를 Docker와 Jenkins로 자동 배포하는 시스템입니다.

## 🏗️ 아키텍처
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React App     │    │  Spring Boot    │    │     MySQL       │
│   (Nginx)       │◄──►│     API         │◄──►│   Database      │
│   Port: 80      │    │   Port: 8080    │    │   Port: 3306    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │    Jenkins      │
                    │   CI/CD         │
                    │   Port: 8081    │
                    └─────────────────┘
```

## 📁 생성된 파일 구조
```
📁 프로젝트 루트/
├── 📄 docker-compose.yml          # 전체 서비스 오케스트레이션
├── 📄 .env                        # 환경 변수 설정
├── 📄 deploy.sh                   # 배포 스크립트
├── 📄 Jenkinsfile                 # Jenkins 파이프라인
├── 📁 back/
│   └── 📄 Dockerfile              # Spring Boot 컨테이너
├── 📁 frontend/
│   ├── 📄 Dockerfile              # React 빌드 컨테이너
│   └── 📄 nginx.conf              # Nginx 설정
├── 📁 jenkins/
│   ├── 📄 Dockerfile              # Jenkins 컨테이너
│   ├── 📄 plugins.txt             # Jenkins 플러그인 목록
│   └── 📁 jobs/
│       └── 📄 stock-pipeline.xml  # Jenkins Job 설정
└── 📁 scripts/
    └── 📄 health-check.sh         # 헬스체크 스크립트
```

## 🚀 배포 방법

### 1. 사전 준비
```bash
# Docker 및 Docker Compose 설치 확인
docker --version
docker-compose --version

# Git 저장소 클론 (필요시)
git clone <your-repository-url>
cd S13P21A301
```

### 2. 환경 변수 설정
```bash
# .env 파일 수정 (필요시)
# DB_PASSWORD, JENKINS_ADMIN_PASSWORD 등 보안 설정
```

### 3. 배포 실행
```bash
# Linux/Mac
./deploy.sh

# Windows (Git Bash 또는 WSL)
bash deploy.sh

# 또는 직접 Docker Compose 실행
docker-compose up -d
```

### 4. 서비스 확인
```bash
# 헬스체크 실행
bash scripts/health-check.sh

# 또는 개별 확인
curl http://localhost/health          # 프론트엔드
curl http://localhost:8080/actuator/health  # 백엔드
curl http://localhost:8081/login      # Jenkins
```

## 🌐 서비스 접속 정보

| 서비스 | URL | 포트 | 설명 |
|--------|-----|------|------|
| **프론트엔드** | http://localhost | 80 | React 앱 (Nginx) |
| **백엔드 API** | http://localhost:8080 | 8080 | Spring Boot API |
| **Jenkins** | http://localhost:8081 | 8081 | CI/CD 파이프라인 |
| **MySQL** | localhost | 3306 | 데이터베이스 |

## 🔧 Jenkins 설정

### 1. 초기 접속
1. http://localhost:8081 접속
2. 초기 비밀번호 확인:
   ```bash
   docker exec stock_jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```

### 2. 플러그인 설치
- "Install suggested plugins" 선택
- 또는 `jenkins/plugins.txt`에 정의된 플러그인들 설치

### 3. 파이프라인 설정
1. "New Item" → "Pipeline" 선택
2. Pipeline script from SCM 선택
3. Git 저장소 URL 입력
4. Script Path: `Jenkinsfile`

## 🔄 CI/CD 파이프라인

### 자동 트리거
- **Git Push**: 코드 푸시 시 자동 빌드/배포
- **스케줄**: 5분마다 변경사항 확인

### 파이프라인 단계
1. **Checkout**: 코드 체크아웃
2. **Build Backend**: Spring Boot 빌드
3. **Build Frontend**: React 빌드
4. **Test**: 단위 테스트 실행
5. **Deploy**: Docker 컨테이너 배포
6. **Health Check**: 서비스 상태 확인

## 🛠️ 개발 환경 설정

### 로컬 개발
```bash
# 백엔드 개발 서버
cd back
./gradlew bootRun

# 프론트엔드 개발 서버
cd frontend
npm run dev
```

### Docker 개발 환경
```bash
# 개발용 Docker Compose (필요시 생성)
docker-compose -f docker-compose.dev.yml up -d
```

## 📊 모니터링

### 로그 확인
```bash
# 전체 서비스 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f spring-backend
docker-compose logs -f react-frontend
docker-compose logs -f jenkins
```

### 리소스 모니터링
```bash
# 컨테이너 상태
docker-compose ps

# 리소스 사용량
docker stats

# 헬스체크
bash scripts/health-check.sh
```

## 🔒 보안 설정

### 환경 변수 보안
```bash
# .env 파일 권한 설정 (Linux/Mac)
chmod 600 .env

# 프로덕션 환경에서는 더 강력한 비밀번호 사용
DB_PASSWORD=your_secure_password_here
JENKINS_ADMIN_PASSWORD=your_jenkins_password_here
```

### 방화벽 설정
```bash
# 필요한 포트만 열기
# 80 (HTTP), 443 (HTTPS), 22 (SSH), 8081 (Jenkins)
```

## 🚨 문제 해결

### 일반적인 문제들

1. **포트 충돌**
   ```bash
   # 포트 사용 중인 프로세스 확인
   netstat -tulpn | grep :80
   netstat -tulpn | grep :8080
   ```

2. **Docker 권한 문제**
   ```bash
   # Docker 그룹에 사용자 추가 (Linux)
   sudo usermod -aG docker $USER
   ```

3. **MySQL 연결 실패**
   ```bash
   # MySQL 컨테이너 상태 확인
   docker-compose logs mysql
   ```

4. **빌드 실패**
   ```bash
   # 캐시 정리 후 재빌드
   docker-compose build --no-cache
   ```

### 로그 위치
- **애플리케이션 로그**: `docker-compose logs [service-name]`
- **Jenkins 로그**: `docker exec stock_jenkins tail -f /var/jenkins_home/jenkins.log`
- **MySQL 로그**: `docker-compose logs mysql`

## 📞 지원

문제가 발생하면 다음을 확인해주세요:
1. Docker 및 Docker Compose 버전
2. 시스템 리소스 (메모리, 디스크 공간)
3. 네트워크 연결 상태
4. 방화벽 설정

---

🎉 **배포 완료!** 이제 자동화된 CI/CD 파이프라인을 통해 개발을 시작할 수 있습니다!

