# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai"
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/target/reno-waste.jar /app/reno-waste.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD curl -fsS http://localhost:8080/api/public/health || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/reno-waste.jar"]
