# BE 서버용 Dockerfile
FROM openjdk:17-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# 모든 파일 복사
COPY . .

# Gradle 빌드 실행 (상세 로그 포함)
RUN chmod +x ./gradlew
RUN ./gradlew build -x test --info

# 빌드 결과 확인
RUN echo "=== 빌드 디렉토리 확인 ===" && \
    ls -la && \
    echo "=== build 디렉토리 확인 ===" && \
    ls -la build/ && \
    echo "=== build/libs 디렉토리 확인 ===" && \
    ls -la build/libs/ || echo "build/libs 디렉토리가 없습니다!"

# JAR 파일을 app.jar로 복사 (안전한 방법)
RUN if [ -d "build/libs" ]; then \
        JAR_FILE=$(find build/libs -name "*.jar" -type f | head -1) && \
        echo "JAR 파일 찾음: $JAR_FILE" && \
        cp "$JAR_FILE" app.jar && \
        ls -la app.jar; \
    else \
        echo "build/libs 디렉토리가 없습니다. 빌드가 실패했을 수 있습니다." && \
        exit 1; \
    fi

# 포트 노출
EXPOSE 8080

# 환경변수 설정
ENV PORT=8080

# 헬스 체크 추가
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
