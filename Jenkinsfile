pipeline {
  agent any
  options { timestamps() }

  environment {
    TZ = 'Asia/Seoul'
    // 이미지 기본명
    BACKEND_IMAGE_BASE = 'stock-backend'
    FRONTEND_IMAGE_BASE = 'stock-frontend'
    // 태그는 Git 커밋 해시 사용
    TAG = "${GIT_COMMIT}"

    // 실제 배포에 사용될 태그
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

    stage('Unit Tests (backend)') {
      steps {
        dir('back') {
          sh 'chmod +x gradlew || true'
          sh './gradlew test --no-daemon'
        }
      }
      post {
        always {
          junit testResults: 'back/build/test-results/test/*.xml', allowEmptyResults: true
        }
      }
    }

    stage('Build Images') {
      parallel {
        stage('Build Backend') {
          steps {
            dir('back') {
              sh 'docker build -t ${APP_IMAGE} .'
              sh 'docker tag ${APP_IMAGE} ${BACKEND_IMAGE_BASE}:latest'
            }
          }
        }
        stage('Build Frontend') {
          when { expression { fileExists('frontend/Dockerfile') } }
          steps {
            dir('frontend') {
              sh 'docker build -t ${WEB_IMAGE} .'
              sh 'docker tag ${WEB_IMAGE} ${FRONTEND_IMAGE_BASE}:latest'
            }
          }
        }
      }
    }

    stage('Deploy (docker compose)') {
      steps {
        dir('/workspace') {   // 호스트의 /srv/app 이 여기에 마운트됨
          sh '''
            set -e
            # .env에 방금 빌드한 이미지 태그 주입(없으면 추가)
            sed -i "s/^APP_IMAGE=.*/APP_IMAGE=${APP_IMAGE}/;t; $ a APP_IMAGE=${APP_IMAGE}" .env || true
            sed -i "s/^WEB_IMAGE=.*/WEB_IMAGE=${WEB_IMAGE}/;t; $ a WEB_IMAGE=${WEB_IMAGE}" .env || true

            # app/web 프로파일 켜서 배포
            docker compose --profile app --profile web up -d
            docker compose ps
          '''
        }
      }
    }

    stage('Health Check (app)') {
      steps {
        sh '''
          set -e
          # docker compose ps -q app로 CID 얻기
          CID=$(docker compose ps -q app)
          if [ -z "$CID" ]; then
            echo "App container not found"
            exit 1
          fi
          
          echo "App container ID: $CID"
          for i in $(seq 1 30); do
            STATUS=$(docker inspect -f '{{.State.Health.Status}}' "$CID" 2>/dev/null || echo "unknown")
            echo "app status: $STATUS (attempt $i/30)"
            if [ "$STATUS" = "healthy" ]; then
              echo "App is healthy!"
              exit 0
            fi
            sleep 5
          done
          echo "App not healthy after 30 attempts"
          docker logs "$CID" --tail=200 || true
          exit 1
        '''
      }
    }
  }

  post {
    success {
      echo "배포 성공 ✅  (tag=${GIT_COMMIT})"
    }
    failure {
      echo "배포 실패 ❌"
      sh 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Image}}" || true'
    }
    always {
      // 공간 회수(선택)
      sh 'docker system prune -f || true'
    }
  }
}

