# 多阶段构建：Maven 编译 → JRE 运行
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# 使用阿里云镜像加速依赖下载（海外环境可删除下一行）
COPY maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/waste-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
