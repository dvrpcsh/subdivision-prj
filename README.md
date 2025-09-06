# 🛒 우리동네 공동구매 플랫폼 (Project: Dongne-Gonggu)

내 주변 이웃들과 함께 소분 상품을 공동구매하고 소통할 수 있는 위치 기반
공동구매 플랫폼입니다.

## ✨ 주요 기능 (Key Features)

-   📍 **위치 기반 팟(Pod) 조회**: 사용자 현재 위치(위도/경도)를
    기준으로 1km \~ 30km 반경 내에 생성된 공동구매(팟) 목록을 실시간으로
    조회합니다.
-   ✍️ **팟 생성 및 관리 (CRUD)**: 공동구매하고 싶은 상품에 대한 팟을
    직접 생성하고, 참여 인원, 상품 정보 등을 자유롭게 수정 및 관리할 수
    있습니다.
-   🤝 **팟 참여 및 상태 관리**: 원하는 팟에 참여하거나 참여를 취소할 수
    있으며, 모집 인원이 다 차면 '모집 완료' 상태로 자동 변경됩니다.
-   💬 **실시간 채팅**: **WebSocket(STOMP)**을 기반으로 팟 참여자들끼리
    실시간으로 소통할 수 있는 채팅 기능을 제공합니다. (입장/대화 메시지
    분리 처리)
-   🔍 **동적 검색 및 필터링**: JPA Specification을 활용하여
    키워드(제목, 내용, 상품명), 카테고리, 모집 상태 등 다양한 조건으로
    팟을 동적으로 검색할 수 있습니다.
-   🔐 **안전한 인증 시스템**: JWT 토큰 기반의 자체 로그인/회원가입과
    OAuth2 (Google, Kakao, Naver) 소셜 로그인을 모두 지원하여 사용자
    편의성을 높였습니다.
-   🖼️ **이미지 업로드**: AWS S3와 연동하여 팟 관련 이미지를 안정적으로
    업로드하고, Presigned URL을 통해 안전하게 이미지를 조회합니다.
-   📧 **이메일 인증**: 회원가입 시 이메일 인증을 통해 사용자의 신원을
    확인하고 계정의 보안을 강화합니다.

## 🛠️ 기술 스택 (Tech Stack)

### Backend

-   **Language**: Java 17
-   **Framework**: Spring Boot 3.5.4
-   **Data Access**: Spring Data JPA, Hibernate Spatial
-   **Database**: MySQL (AWS RDS), Redis (Jedis Client)
-   **Real-time**: Spring WebSocket, STOMP
-   **Authentication**: Spring Security, JWT (jjwt), OAuth2
-   **Cloud**: AWS S3, AWS EC2
-   **Build Tool**: Gradle

### DevOps

-   **CI/CD**: GitHub Actions
-   **Containerization**: Docker, Docker Compose
-   **Infrastructure**: AWS EC2, AWS RDS

## 🏗️ 시스템 아키텍처 (Architecture)

간단한 아키텍처 다이어그램을 추가하면 프로젝트를 이해하는 데 큰 도움이
됩니다.

-   **사용자 (Client)**: 웹 브라우저를 통해 서비스에 접속합니다.
-   **AWS EC2**: Spring Boot 애플리케이션이 Docker 컨테이너 형태로
    실행되는 메인 서버입니다.
-   **Spring Boot Application**: 비즈니스 로직, API 엔드포인트,
    WebSocket 통신 등을 처리합니다.
-   **AWS RDS (MySQL)**: 사용자, 팟, 채팅 메시지 등 핵심 데이터를
    영구적으로 저장하는 데이터베이스입니다.
-   **Redis**: 채팅 세션 정보, 캐시 데이터 등 휘발성이지만 빠른 접근이
    필요한 데이터를 관리합니다.
-   **AWS S3**: 사용자가 업로드하는 이미지를 저장하는 안정적인
    스토리지입니다.
-   **GitHub & Docker Hub**: master 브랜치에 코드 Push 시 GitHub
    Actions가 자동으로 앱을 빌드하고, Docker 이미지를 생성하여 Docker
    Hub에 푸시한 뒤, EC2에 배포하는 CI/CD 파이프라인이 구축되어
    있습니다.

## 🌐 홈페이지

[www.dongne-gonggu.shop](http://www.dongne-gonggu.shop)
