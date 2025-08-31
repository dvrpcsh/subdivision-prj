#!/bin/bash
# ===================================================================
# AWS EC2 서버에서 실행될 배포 스크립트
# ===================================================================

# 1. 변수 설정
# Docker Hub 사용자 이름과 프로젝트 이름을 변수로 지정합니다.
DOCKER_USERNAME=$1
DOCKER_APP_NAME=$2
# .env 파일의 경로를 지정합니다.
ENV_PATH=$3

# 2. Docker Hub 로그인
# GitHub Secrets를 통해 전달받은 Docker Hub 사용자 이름과 토큰으로 로그인합니다.
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

# 4. 기존 컨테이너 중지 및 재시작
# .env 파일을 참조하여 docker-compose로 컨테이너를 실행합니다.
# --env-file 옵션을 사용하여 환경 변수 파일의 위치를 명시적으로 지정합니다.
echo ">> Docker Compose 로 애플리케이션 재시작..."
docker-compose --env-file $ENV_PATH up -d
if [ $? -ne 0 ]; then
    echo ">> 애플리케이션 재시작 실패"
    exit 1
fi
echo ">> 애플리케이션 재시작 성공"

# 5. 불필요한(dangling) Docker 이미지 삭제
# 이전 버전의 이미지가 서버에 계속 쌓이는 것을 방지하여 용량을 확보합니다.
echo ">> 불필요한 이미지 정리..."
docker image prune -f

echo ">> 배포 성공적으로 완료!"

