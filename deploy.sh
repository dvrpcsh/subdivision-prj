#!/bin/bash
# ===================================================================
# AWS EC2 서버에서 실행될 배포 스크립트 (최종 안정화 버전)
# ===================================================================

# 1. 변수 설정 (스크립트 실행 시 GitHub Actions 워크플로우로부터 전달받음)
DOCKER_USERNAME=$1
DOCKER_APP_NAME=$2
ENV_PATH=$3

# 2. Docker Hub 로그인
echo ">> Docker Hub 로그인 시도..."
# GitHub Secrets를 통해 받은 토큰을 표준 입력(stdin)으로 전달하여 안전하게 로그인합니다.
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
# $?는 바로 이전 명령어의 종료 코드를 의미합니다. 0이 아니면 실패입니다.
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

# 4. Docker Compose로 애플리케이션 재시작
# 최신 Docker Compose V2 문법인 'docker compose' (띄어쓰기)를 사용합니다.
# --env-file 옵션으로 환경 변수 파일의 위치를 명시합니다.
# -d 옵션으로 백그라운드에서 실행합니다.
echo ">> Docker Compose 로 애플리케이션 재시작..."
docker compose --env-file $ENV_PATH up -d
if [ $? -ne 0 ]; then
    echo ">> 애플리케이션 재시작 명령어 실행 실패"
    exit 1
fi
echo ">> 애플리케이션 재시작 명령어 실행 성공"

# 💡 [핵심 개선] 5. 배포 후 애플리케이션이 정말 실행되었는지 상태를 확인합니다.
# 컨테이너가 켜지는 데 약간의 시간이 걸릴 수 있으므로 10초간 대기합니다.
echo ">> 10초 후 애플리케이션 상태 확인..."
sleep 10

# `docker ps`로 실행 중인 컨테이너 목록을 확인하고,
# `grep subdivision-app`으로 우리 앱 컨테이너가 있는지 필터링합니다.
# `grep -q` 옵션은 결과가 있으면 0, 없으면 1을 반환합니다.
if ! docker ps | grep -q "subdivision-app"; then
    echo ">> 오류: subdivision-app 컨테이너가 실행되지 않았습니다."
    # 컨테이너가 켜지지 않았다면, 실패 원인을 파악할 수 있도록 상세한 로그를 출력합니다.
    echo ">> subdivision-app 컨테이너의 로그를 출력합니다:"
    docker logs subdivision-app
    # 파이프라인을 실패 처리합니다.
    exit 1
else
    echo ">> subdivision-app 컨테이너가 정상적으로 실행 중입니다."
fi

# 6. 불필요한(dangling) Docker 이미지 삭제
# 이전 버전의 이미지가 서버에 계속 쌓이는 것을 방지하여 용량을 확보합니다.
echo ">> 불필요한 이미지 정리..."
docker image prune -f

echo ">> 배포 성공적으로 완료!"

