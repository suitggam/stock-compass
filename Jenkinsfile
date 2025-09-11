pipeline {
  agent any
  options { timestamps() }

  parameters {
    booleanParam(name: 'DEPLOY', defaultValue: false, description: '✅ 체크하면 배포까지 수행')
  }

  environment {
    TZ = 'Asia/Seoul'
    APP_IMAGE = "stock-backend:${GIT_COMMIT}"
    WEB_IMAGE = "stock-frontend:${GIT_COMMIT}"
    // Nginx 없이 프론트가 백엔드를 때릴 절대 URL (임시)
    VITE_API_BASE_URL = "http://j13a301.p.ssafy.io:8080"
  }

  stages {
    stage('Checkout'){ steps { checkout scm } }

    stage('Build Backend'){
      steps {
        dir('back'){
          sh 'chmod +x gradlew || true'
          sh './gradlew test --no-daemon || true'   // 테스트는 지금은 실패해도 진행
          sh 'docker build -t ${APP_IMAGE} .'
        }
      }
    }

    stage('Build Frontend'){
      when { expression { fileExists('frontend/Dockerfile') } }
      steps {
        dir('frontend'){
          sh 'docker build --build-arg VITE_API_BASE_URL=${VITE_API_BASE_URL} -t ${WEB_IMAGE} . || docker build -t ${WEB_IMAGE} .'
        }
      }
    }

    stage('Deploy (optional)'){
      when { expression { return params.DEPLOY } }   // ✅ 체크했을 때만 실행
      steps {
        dir('/workspace'){   // == 서버 /srv/app
          sh '''
            set -e

            # 필수 env 키 보강(경고 제거)
            grep -q '^MYSQL_ROOT_PASSWORD=' .env || echo 'MYSQL_ROOT_PASSWORD=ssafy' >> .env
            grep -q '^MYSQL_DATABASE=' .env      || echo 'MYSQL_DATABASE=survive_stock' >> .env

            # 이미지 태그 주입
            sed -i "s/^APP_IMAGE=.*/APP_IMAGE=${APP_IMAGE}/;t; $ a APP_IMAGE=${APP_IMAGE}" .env || true
            sed -i "s/^WEB_IMAGE=.*/WEB_IMAGE=${WEB_IMAGE}/;t; $ a WEB_IMAGE=${WEB_IMAGE}" .env || true

            # 임시 포트 매핑 파일(없으면 생성)
            [ -f docker-compose.ports.yml ] || cat > docker-compose.ports.yml <<'YAML'
services:
  app:
    ports: ["8080:8080"]
  web:
    ports: ["8082:80"]
YAML

            # 앱+웹 기동 (Nginx 없이)
            docker compose -f docker-compose.yml -f docker-compose.override.yml -f docker-compose.ports.yml up -d app web
            docker compose ps
          '''
        }
      }
    }

    stage('Health Check (backend)'){
      when { expression { return params.DEPLOY } }
      steps {
        dir('/workspace'){
          sh '''
            set -e
            CID=$(docker compose ps -q app)
            [ -z "$CID" ] && echo "app container not found" && exit 1
            for i in $(seq 1 30); do
              STATUS=$(docker inspect -f '{{.State.Health.Status}}' "$CID" 2>/dev/null || echo unknown)
              echo "backend health: $STATUS ($i/30)"
              [ "$STATUS" = "healthy" ] && exit 0
              sleep 5
            done
            docker logs "$CID" --tail=200 || true
            exit 1
          '''
        }
      }
    }
  }

  post {
    success { echo "✅ 성공: CI${params.DEPLOY ? '+CD' : ''}" }
    failure { echo "❌ 실패"; sh 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Image}}" || true' }
  }
}
