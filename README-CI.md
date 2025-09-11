# 🚀 CI/CD 자동 배포 시스템

## 📋 개요
Git push → Jenkins가 서버(/srv/app)에서 docker compose로 자동 배포하는 시스템입니다.

## 🏗️ 아키텍처
```
Git Push → Jenkins (빌드) → Docker Registry → 서버(/srv/app) → Docker Compose 배포
```

## 📁 파일 구조
```
📁 프로젝트 루트/
├── 📄 Jenkinsfile                 # Jenkins 파이프라인
├── 📄 docker-compose.ci.yml       # CI/CD용 Docker Compose
├── 📁 back/
│   ├── 📄 Dockerfile              # Spring Boot 멀티스테이지 빌드
│   └── 📄 .dockerignore           # Docker 빌드 최적화
├── 📁 frontend/
│   ├── 📄 Dockerfile              # React + Nginx 빌드
│   ├── 📄 nginx.conf              # Nginx 설정 (상대경로 API 호출)
│   └── 📄 .dockerignore           # Docker 빌드 최적화
└── 📄 README-CI.md                # 이 파일
```

## 🔄 CI/CD 파이프라인

### 1. **Checkout**
- Git 저장소에서 최신 코드 체크아웃

### 2. **Unit Tests (backend)**
- Spring Boot 백엔드 단위 테스트 실행
- `./gradlew test --no-daemon`

### 3. **Build Images**
- **Build Backend**: `docker build -t stock-backend:${GIT_COMMIT} back`
- **Build Frontend**: `docker build -t stock-frontend:${GIT_COMMIT} frontend` (있으면)

### 4. **Deploy (docker compose)**
- Jenkins 컨테이너에서 `/workspace`(= 서버 /srv/app)로 이동
- `.env`의 `APP_IMAGE`/`WEB_IMAGE`를 방금 빌드한 태그로 갱신
- `docker compose --profile app --profile web up -d` 실행

### 5. **Health Check (app)**
- `docker compose ps -q app`로 컨테이너 ID 획득
- `docker inspect -f '{{.State.Health.Status}}' "$CID"`가 `healthy` 될 때까지 대기

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
# npm ci, npm run build

FROM nginx:1.27-alpine
# 정적 파일 서빙, HEALTHCHECK: index.html 존재 확인
```

## 🔧 환경 변수

### Jenkins 환경 변수
```bash
TAG = "${GIT_COMMIT}"                    # Git 커밋 해시
APP_IMAGE = "stock-backend:${GIT_COMMIT}" # 백엔드 이미지
WEB_IMAGE = "stock-frontend:${GIT_COMMIT}" # 프론트엔드 이미지
```

### 서버 .env 파일
```bash
# 데이터베이스
DB_NAME=stock_db
DB_USER=stock_user
DB_PASSWORD=stock123!

# 이미지 태그 (Jenkins가 자동 갱신)
APP_IMAGE=stock-backend:${GIT_COMMIT}
WEB_IMAGE=stock-frontend:${GIT_COMMIT}

# Spring Boot
SPRING_PROFILE=prod
```

## 🌐 네트워크 설정

### 프론트엔드 API 호출
- **상대경로 사용**: `/api/*` → `http://app:8080/*`
- **절대경로 금지**: `http://localhost:8080` 사용 안함
- **Nginx 프록시**: `/api/` 요청을 백엔드로 전달

### Docker Compose 네트워크
```yaml
networks:
  stock_network:
    driver: bridge
```

## 🚀 배포 프로세스

### 1. 개발자 작업
```bash
git add .
git commit -m "feat: 새로운 기능 추가"
git push origin main
```

### 2. Jenkins 자동 실행
1. **Webhook 트리거** → Jenkins 파이프라인 시작
2. **코드 체크아웃** → 최신 코드 가져오기
3. **테스트 실행** → 백엔드 단위 테스트
4. **이미지 빌드** → Docker 이미지 생성
5. **배포 실행** → 서버에서 컨테이너 재시작
6. **헬스체크** → 서비스 정상 동작 확인

### 3. 서버 상태 확인
```bash
# 서버에서 실행
cd /srv/app
docker compose ps
docker compose logs -f app
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

🎉 **자동 배포 시스템 완성!** 이제 Git push만 하면 자동으로 배포됩니다!
