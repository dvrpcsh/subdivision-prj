# 로컬에서 미리 빌드한 JAR 파일을 사용하는 방식
FROM openjdk:17-jdk-slim

WORKDIR /app

# 실행 가능한 JAR 파일만 복사 (plain.jar 제외)
COPY build/libs/subdivision-prj-0.0.1-SNAPSHOT.jar app.jar

# 8080 포트 노출
EXPOSE 8080

# 애플리케이션 실행 (local 프로파일 사용 - docker-compose와 일치)
ENTRYPOINT ["java", "-Dspring.profiles.active=local", "-jar", "app.jar"]