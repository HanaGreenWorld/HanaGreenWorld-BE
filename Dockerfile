# 1. 빌드 스테이지
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# 모든 파일 복사
# Git에 gradlew 파일이 커밋되었는지 확인하십시오.
COPY . .

# Gradle 빌드 실행
RUN chmod +x ./gradlew
# 테스트를 건너뛰고 빌드하여 빌드 시간을 단축하고 오류를 줄입니다.
RUN ./gradlew build -x test --info

# 2. 실행 스테이지 (경량 JRE 이미지 사용)
# 빌드 이미지보다 작은 JRE 이미지를 사용하여 컨테이너 크기를 줄입니다.
FROM openjdk:17-jre-slim

# Cloud Run은 항상 환경 변수 PORT가 설정되기를 기대합니다 (기본값은 8080).
# 스프링 부트는 이 환경 변수를 사용하도록 명시적으로 지시해야 합니다.
ENV PORT=8080

WORKDIR /app

# 빌더 스테이지에서 생성된 JAR 파일을 복사합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# Cloud Run의 자체 헬스 체크를 사용하므로 Dockerfile의 HEALTHCHECK는 제거합니다.

# 애플리케이션 실행: -Dserver.port=$PORT 인자를 사용하여 스프링 부트가 PORT 환경 변수를 읽도록 합니다.
ENTRYPOINT ["java", "-Dserver.port=$PORT", "-jar", "app.jar"]
