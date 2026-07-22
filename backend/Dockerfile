# 采用轻量级的 OpenJDK 21 运行时镜像
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 注意：请确保复制的是不带 -plain 的可执行 jar 包
COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

# 暴露 Spring Boot 服务端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]