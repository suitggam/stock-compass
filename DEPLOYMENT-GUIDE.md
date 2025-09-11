# 🚀 CI/CD 자동 배포 가이드

## 📋 개요
Git push → Jenkins가 서버(/srv/app)에서 docker compose로 자동 배포하는 시스템입니다.

## 🔄 CI/CD 파이프라인

### 1. **Checkout**
- Git 저장소에서 최신 코드 체크아웃

### 2. **Build Backend**
- Spring Boot 백엔드 빌드
- `docker build -t stock-backend:${GIT_COMMIT} back`
- 테스트 실행 (실패해도 진행)

### 3. **Build Frontend**
- React 프론트엔드 빌드 (있으면)
- `docker build --build-arg VITE_API_BASE_URL=${VITE_API_BASE_URL} -t stock-frontend:${GIT_COMMIT} frontend`

### 4. **Deploy (optional)** ⚠️
- **매개변수로 제어**: `DEPLOY` 체크박스가 true일 때만 실행
- 서버 `/srv/app`에서 Docker Compose로 배포
- `.env` 파일 자동 생성/갱신
- 이미지 태그 자동 주입

### 5. **Health Check (backend)**
- 백엔드 컨테이너 헬스체크
- `healthy` 상태가 될 때까지 대기 (최대 30회, 5초 간격)

## 🐳 Docker 설정

### 백엔드 (back/Dockerfile)
```dockerfile
# 멀티스테이지 빌드
FROM openjdk:17-jdk-slim as builder
# gradlew 권한 설정, 의존성 캐시, build -x test

FROM openjdk:17-jre-slim
# tzdata + curl 설치, HEALTHCHECK: actuator/health
```

### 프론트엔드 (frontend/Dockerfile)
```dockerfile
# 멀티스테이지 빌드
FROM node:18-alpine as builder
# build-arg VITE_API_BASE_URL 지원

FROM nginx:1.27-alpine
# 정적 파일 서빙, HEALTHCHECK: index.html 존재 확인
```

## 🔧 환경 변수

### Jenkins 환경 변수
```bash
APP_IMAGE = "stock-backend:${GIT_COMMIT}"
WEB_IMAGE = "stock-frontend:${GIT_COMMIT}"
VITE_API_BASE_URL = "http://j13a301.p.ssafy.io:8080"  # 임시 절대 URL
```

### 서버 .env 파일 (자동 생성)
```bash
# Database
MYSQL_ROOT_PASSWORD=ssafy
MYSQL_DATABASE=survive_stock
MYSQL_USER=stock_user
MYSQL_PASSWORD=ssafy

# Docker Images (Jenkins가 자동 갱신)
APP_IMAGE=stock-backend:${GIT_COMMIT}
WEB_IMAGE=stock-frontend:${GIT_COMMIT}

# Frontend
VITE_API_BASE_URL=/api
```

## 🌐 네트워크 설정

### 프론트엔드 API 호출
- **개발 시**: `http://j13a301.p.ssafy.io:8080` (절대 URL)
- **배포 시**: `/api/*` → `http://app:8080/*` (상대경로, Nginx 프록시)

### Docker Compose 네트워크
```yaml
networks:
  stock_network:
    driver: bridge
```

## 🚀 사용 방법

### 1. **CI만 실행** (기본)
```bash
git add .
git commit -m "feat: 새로운 기능"
git push origin main
```
- 빌드만 수행, 배포는 하지 않음

### 2. **CI + CD 실행** (배포 포함)
1. Jenkins 웹 UI 접속
2. "Build with Parameters" 클릭
3. `DEPLOY` 체크박스 체크
4. "Build" 클릭

### 3. **서버 상태 확인**
```bash
# 서버에서 실행
cd /srv/app
docker compose ps
docker compose logs -f app
```

## 📁 파일 구조

```
📁 프로젝트 루트/
├── 📄 Jenkinsfile                 # Jenkins 파이프라인
├── 📄 docker-compose.yml          # 메인 Docker Compose
├── 📄 docker-compose.override.yml # 포트 매핑 오버라이드
├── 📄 .env.template               # 환경 변수 템플릿
├── 📁 back/
│   ├── 📄 Dockerfile              # Spring Boot 멀티스테이지 빌드
│   └── 📄 .dockerignore           # Docker 빌드 최적화
├── 📁 frontend/
│   ├── 📄 Dockerfile              # React + Nginx 빌드
│   ├── 📄 nginx.conf              # Nginx 설정 (상대경로 API 호출)
│   └── 📄 .dockerignore           # Docker 빌드 최적화
└── 📄 DEPLOYMENT-GUIDE.md         # 이 파일
```

## 🔍 모니터링

### 헬스체크 엔드포인트
- **백엔드**: `http://localhost:8080/actuator/health`
- **프론트엔드**: `test -f /usr/share/nginx/html/index.html`

### 로그 확인
```bash
# 전체 로그
docker compose logs -f

# 특정 서비스 로그
docker compose logs -f app
docker compose logs -f web
docker compose logs -f db
```

### 컨테이너 상태
```bash
# 실행 중인 컨테이너
docker compose ps

# 리소스 사용량
docker stats
```

## 🛠️ 문제 해결

### 일반적인 문제들

1. **빌드 실패**
   ```bash
   # Jenkins에서 확인
   docker build -t stock-backend:test back
   ```

2. **헬스체크 실패**
   ```bash
   # 컨테이너 상태 확인
   docker inspect <container_id>
   ```

3. **네트워크 연결 문제**
   ```bash
   # 네트워크 확인
   docker network ls
   docker network inspect stock_network
   ```

4. **이미지 태그 문제**
   ```bash
   # 이미지 목록 확인
   docker images | grep stock
   ```

### 로그 위치
- **Jenkins 로그**: Jenkins 웹 UI → Build → Console Output
- **애플리케이션 로그**: `docker compose logs [service-name]`
- **Docker 로그**: `/var/log/docker.log`

## 📞 지원

문제가 발생하면 다음을 확인해주세요:
1. Jenkins 빌드 로그
2. Docker 컨테이너 상태
3. 네트워크 연결
4. 환경 변수 설정

---

🎉 **자동 배포 시스템 완성!** 
- **CI**: Git push 시 자동 빌드
- **CD**: 매개변수로 선택적 배포
