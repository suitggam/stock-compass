pipeline {
  agent any
  options { timestamps() }

  environment {
    TZ = 'Asia/Seoul'
    APP_IMAGE = "stock-backend:${GIT_COMMIT}"
    WEB_IMAGE = "stock-frontend:${GIT_COMMIT}"
  }

  stages {
    stage('Checkout') {
      steps {
        echo '코드 체크아웃...'
        checkout scm
      }
    }

    stage('Build Backend') {
      steps {
        echo 'Spring Boot 백엔드 빌드...'
        dir('back') {
          sh 'chmod +x gradlew || true'
          sh 'docker build -t ${APP_IMAGE} .'
        }
      }
    }

    stage('Build Frontend') {
      when { expression { fileExists('frontend/Dockerfile') } }
      steps {
        echo 'React 프론트엔드 빌드...'
        dir('frontend') {
          sh 'docker build -t ${WEB_IMAGE} .'
        }
      }
    }

    stage('Deploy') {
      steps {
        echo '서비스 배포...'
        dir('/workspace') {   // 서버 /srv/app 마운트
          sh '''
            set -e

            # .env 파일이 없으면 예시에서 복사
            [ -f .env ] || cp env.example .env

            # 이미지 태그 갱신
            sed -i "s/^APP_IMAGE=.*/APP_IMAGE=${APP_IMAGE}/" .env || echo "APP_IMAGE=${APP_IMAGE}" >> .env
            sed -i "s/^WEB_IMAGE=.*/WEB_IMAGE=${WEB_IMAGE}/" .env || echo "WEB_IMAGE=${WEB_IMAGE}" >> .env

            # 서비스 배포
            docker compose up -d app web nginx
            docker compose ps
          '''
        }
      }
    }
  }

  post {
    success {
      echo "✅ 배포 성공! (tag=${GIT_COMMIT})"
    }
    failure {
      echo "❌ 배포 실패!"
      sh 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Image}}" || true'
    }
    always {
      sh 'docker system prune -f || true'
    }
  }
}
