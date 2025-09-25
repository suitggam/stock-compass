pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(
      daysToKeepStr: '14',
      numToKeepStr: '50',
      artifactDaysToKeepStr: '14',
      artifactNumToKeepStr: '20'
    ))
    timeout(time: 60, unit: 'MINUTES')
    durabilityHint('PERFORMANCE_OPTIMIZED')
    skipDefaultCheckout(true)   // 원격에서 git clone 하므로 기본 체크아웃 생략
  }

  // NOTE: GitLab 푸시는 잡 설정(UI)의 GitLab 트리거로 받는 것을 권장
  // triggers { gitlab() }  // 플러그인/설정이 확실하면 이 줄을 활성화

  environment {
    HOST   = '13.125.222.104'
    REPO   = 'https://lab.ssafy.com/s13-bigdata-dist-sub1/S13P21A301.git'
    BRANCH = 'master'
  }

  stages {
    stage('Build & Deploy') {
      options {
        timeout(time: 40, unit: 'MINUTES')
        retry(1)
      }
      steps {
        ansiColor('xterm') {
          withCredentials([
            sshUserPrivateKey(credentialsId: 'prod-ssh', keyFileVariable: 'KEY', usernameVariable: 'USER'),
            usernamePassword(credentialsId: 'gitlab-deploy', usernameVariable: 'GL_USER', passwordVariable: 'GL_PASS'),
            file(credentialsId: 'FRONTEND_ENV', variable: 'FE_ENV_FILE'),
            file(credentialsId: 'BACKEND_ENV',  variable: 'BE_ENV_FILE')
          ]) {
            sh '''
              set -euo pipefail

              echo "==[1/5] 원격 준비 =="
              ssh -o StrictHostKeyChecking=no -i "$KEY" "$USER@${HOST}" '
                set -e
                mkdir -p ~/ci/app/repo/frontend ~/ci/app/repo/back /srv/app/backend /srv/app/frontend
                chmod 700 ~/ci/app/repo/back
                chown $USER:$USER ~/ci/app/repo/back
              '

              echo "==[2/5] .env 업로드 =="
              scp -o StrictHostKeyChecking=no -i "$KEY" "$FE_ENV_FILE" "$USER@${HOST}:~/ci/app/repo/frontend/.env.tmp"
              scp -o StrictHostKeyChecking=no -i "$KEY" "$BE_ENV_FILE" "$USER@${HOST}:~/ci/app/repo/back/.env.tmp"
              ssh -o StrictHostKeyChecking=no -i "$KEY" "$USER@${HOST}" '
                set -e
                mv ~/ci/app/repo/frontend/.env.tmp ~/ci/app/repo/frontend/.env && chmod 600 ~/ci/app/repo/frontend/.env
                mv ~/ci/app/repo/back/.env.tmp     ~/ci/app/repo/back/.env     && chmod 600 ~/ci/app/repo/back/.env
              '

              rm -rf logs && mkdir -p logs

              echo "==[3/5] 원격 빌드/배포 =="
              ssh -o StrictHostKeyChecking=no -i "$KEY" "$USER@${HOST}" GL_USER="$GL_USER" GL_PASS="$GL_PASS" REPO="$REPO" BRANCH="$BRANCH" 'bash -s' <<'EOS'
set -Eeuo pipefail
mkdir -p ~/ci/app ~/ci/logs
cd ~/ci/app

# --- Repo sync ---
if [ ! -d repo ]; then
  git clone --depth=1 --branch "$BRANCH" "https://${GL_USER}:${GL_PASS}@${REPO#https://}" repo
else
  git -C repo fetch --depth=1 origin "$BRANCH"
  git -C repo reset --hard "origin/$BRANCH"
fi
echo "[repo] HEAD: $(git -C repo rev-parse --short HEAD)" | tee ~/ci/logs/clone.log

# 필수 도구
if ! command -v git >/dev/null 2>&1; then
  sudo apt-get update -y && sudo apt-get install -y git openjdk-17-jdk rsync docker-compose-plugin
fi

# --- Backend build ---
(
  set -e
  cd repo/back
  if [ -f ./gradlew ]; then
    chmod +x ./gradlew
    ./gradlew clean bootJar -x test
    JAR=$(ls build/libs/*.jar | head -n1)
  elif [ -f ./mvnw ]; then
    chmod +x ./mvnw
    ./mvnw -DskipTests package
    JAR=$(ls target/*.jar | head -n1)
  elif [ -f pom.xml ]; then
    sudo apt-get update -y && sudo apt-get install -y maven
    mvn -DskipTests package
    JAR=$(ls target/*.jar | head -n1)
  else
    echo "[back][ERR] build tool not found"; exit 2
  fi
  sudo mkdir -p /srv/app/backend
  sudo cp -f "$JAR" /srv/app/backend/app.jar
  echo "[back] artifact: $JAR"
) 2>&1 | tee ~/ci/logs/backend_build.log

# --- Backend image build (compose) ---
(
  set -e
  cd /srv/app
  docker compose build app
) 2>&1 | tee -a ~/ci/logs/backend_build.log

# --- Frontend build ---
(
  set -e
  cd repo/frontend
  # 과거 root 생성물 제거(권한 충돌 방지)
  sudo rm -rf dist build node_modules || true

  # 호스트와 동일 UID/GID로 빌드 → 산출물 소유자 ubuntu
  uid=$(id -u); gid=$(id -g)
  docker run --rm -u ${uid}:${gid} \
    -v "$PWD:/src" -w /src \
    -e VITE_API_BASE_URL=/api \
    -e VITE_SERVER_URL=https://j13a301.p.ssafy.io \
    node:20-bullseye bash -lc '
      set -e
      # corepack/pnpm 전역 심링크는 권한 이슈 → 사용 안 함
      if [ -f package-lock.json ]; then npm ci; else npm i; fi
      npx vite build --mode production
    '

  OUTDIR=""
  [ -d dist ] && OUTDIR="dist"
  [ -z "$OUTDIR" ] && [ -d build ] && OUTDIR="build"
  if [ -z "$OUTDIR" ]; then echo "[front][ERR] build output not found"; exit 21; fi

  # 안전 체크(빌드 중단X, 경고만)
  if grep -RE '://[^[:space:]]*:8080' -n "$OUTDIR"; then
    echo "[front][WARN] :8080 문자열이 산출물에 포함되어 있습니다(개발용 잔재일 수 있음)."
  fi
  if grep -R '/api/api/' -n "$OUTDIR"; then
    echo "[front][WARN] '/api/api/' 중복 경로가 보입니다. 프록시 설정을 확인하세요."
  fi

  sudo mkdir -p /srv/app/frontend/dist
  sudo rsync -a --delete "$OUTDIR/"/ /srv/app/frontend/dist/

  COMMIT=$(git -C .. rev-parse --short HEAD || true)
  DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
  echo "commit=${COMMIT} built_at=${DATE}" | sudo tee /srv/app/frontend/dist/__build.txt >/dev/null
) 2>&1 | tee ~/ci/logs/frontend_build.log

# --- Deploy & smoke ---
(
  set -e
  cd /srv/app
  docker compose up -d app nginx
  docker compose ps
  docker compose exec -T nginx nginx -t || true
  docker compose exec -T nginx nginx -s reload || true

  set +e
  echo -n "HTTP /            : "; curl -sI https://j13a301.p.ssafy.io | head -n1
  echo -n "API Google (302?) : "; curl -sI https://j13a301.p.ssafy.io/api/users/auth/google | grep -i ^location || true
  echo -n "App health (in-c) : "; docker compose exec -T app sh -lc "wget -qO- http://localhost:8080/actuator/health || true" | tr -d '\\n'; echo
) 2>&1 | tee ~/ci/logs/deploy.log
EOS

              echo "==[4/5] 원격 로그 수집 =="
              scp -o StrictHostKeyChecking=no -i "$KEY" "$USER@${HOST}:~/ci/logs/*" ./logs/ || true
            '''
          }
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'logs/**', fingerprint: true, onlyIfSuccessful: false
      echo "⏱ 총 소요: ${currentBuild.durationString}"
    }
    success  { echo '✅ 배포 성공' }
    unstable { echo '⚠️  배포 불안정(UNSTABLE)' }
    failure  { echo '❌ 배포 실패' }
    cleanup  { cleanWs(deleteDirs: true, notFailBuild: true) }
  }
}

