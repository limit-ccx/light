# 运行阶段：使用轻量级 JRE 镜像
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 直接复制本地已编译好的 JAR 包（跳过 Maven 构建）
COPY target/zhiguang-backend-1.0.0.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
