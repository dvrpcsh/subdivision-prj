#!/bin/bash
# ===================================================================
# AWS EC2 서버에서 실행될 배포 스크립트
# ===================================================================

# 1. 변수 설정
DOCKER_USERNAME=$1
DOCKER_APP_NAME=$2
ENV_PATH=$3

# 2. Docker Hub 로그인
echo ">> Docker Hub 로그인 시도..."
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
if [ $? -ne 0 ]; then
    echo ">> Docker Hub 로그인 실패"
    exit 1
fi
echo ">> Docker Hub 로그인 성공"

# 3. 최신 Docker 이미지 받아오기
echo ">> 최신 Docker 이미지 PULL..."
docker pull $DOCKER_USERNAME/$DOCKER_APP_NAME:latest
if [ $? -ne 0 ]; then
    echo ">> 이미지 PULL 실패"
    exit 1
fi
echo ">> 이미지 PULL 성공"

# 4. 💡 [최종 수정] 기존 컨테이너 중지 및 재시작
# 최신 Docker Compose V2 문법인 'docker compose' (띄어쓰기)를 사용합니다.
echo ">> Docker Compose 로 애플리케이션 재시작..."
docker compose --env-file $ENV_PATH up -d
if [ $? -ne 0 ]; then
    echo ">> 애플리케이션 재시작 실패"
    exit 1
fi
echo ">> 애플리케이션 재시작 성공"

# 5. 불필요한 Docker 이미지 삭제
echo ">> 불필요한 이미지 정리..."
docker image prune -f

echo ">> 배포 성공적으로 완료!"

