# ---- ビルドステージ ----
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# 依存関係だけ先にキャッシュ（ソースコード変更時の再ビルドを高速化）
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ---- 実行ステージ ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/my-gae-el-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
