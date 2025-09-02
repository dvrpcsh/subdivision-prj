# 💡 [개선] 1. 베이스 이미지를 Amazon Corretto 17로 변경합니다.
# Amazon Corretto는 AWS 환경에 최적화된 무료 JDK입니다.
FROM amazoncorretto:17-alpine-jdk

# 2. 작업 디렉토리를 설정합니다.
WORKDIR /app

# 4. 빌드된 JAR 파일을 복사합니다.
COPY build/libs/subdivision-prj-0.0.1-SNAPSHOT.jar app.jar

# 5. 8080 포트를 노출합니다.
EXPOSE 8080

# 💡 [핵심] 6. 애플리케이션 실행 명령어에 JVM 메모리 옵션을 추가합니다.
# -Xms256m: 최소 힙 메모리를 256MB로 설정
# -Xmx512m: 최대 힙 메모리를 512MB로 설정 (t2.micro의 1GB RAM을 고려한 안전한 설정)
# 이 설정을 통해 Spring Boot 앱이 서버의 모든 메모리를 차지하는 것을 방지합니다.
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Dspring.profiles.active=local", "-jar", "app.jar"]
