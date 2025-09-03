# [개선] 1. 베이스 이미지를 Amazon Corretto 17로 변경합니다.
# Amazon Corretto는 AWS 환경에 최적화된 무료 JDK입니다.
FROM amazoncorretto:17-alpine-jdk

# 2. 작업 디렉토리를 설정합니다.
WORKDIR /app

# 3. 빌드된 JAR 파일을 복사합니다.
COPY build/libs/subdivision-prj-0.0.1-SNAPSHOT.jar app.jar

# 4. 8080 포트를 노출합니다.
EXPOSE 8080

# 💡 [핵심 수정] 5. 애플리케이션 실행 명령어에서 하드코딩된 프로파일 설정을 제거합니다.
# 기존의 '-Dspring.profiles.active=local' 부분을 삭제했습니다.
# 이제 이 Docker 이미지는 실행될 때 주입되는 환경 변수(SPRING_PROFILES_ACTIVE)를 따르게 됩니다.
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]