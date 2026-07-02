# 第一阶段：构建 jar 包
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 第二阶段：运行
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="周宇斌 <zhouyubin@neu.edu.cn>"
# 设置时区为上海
RUN apk add --no-cache tzdata && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone
# 从 builder 阶段复制打包好的 jar 包
COPY --from=builder /build/target/mall-backend-1.0-SNAPSHOT.jar /app.jar
# 暴露后端端口
EXPOSE 8080
# 启动命令
ENTRYPOINT ["java", "-jar", "/app.jar"]