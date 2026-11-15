# Используем multistage build для уменьшения размера финального образа
FROM eclipse-temurin:17-jdk-alpine AS builder

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем только файлы, необходимые для сборки зависимостей
# Это позволяет кэшировать слой с зависимостями
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Делаем mvnw исполняемым
RUN chmod +x mvnw

# Скачиваем зависимости (кэшируется, если pom.xml не изменился)
RUN ./mvnw dependency:go-offline -B

# Копируем исходный код
COPY src src

# Собираем приложение вместе с тестами: непрошедшая проверка не попадает в образ
RUN ./mvnw package -B

# Финальный stage - только с JRE и собранным jar
FROM eclipse-temurin:17-jre-alpine

# Создаем непривилегированного пользователя для безопасности
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && apk add --no-cache wget

WORKDIR /app

# Копируем только jar файл из builder stage
COPY --from=builder /app/target/english-*.jar app.jar

# Переключаемся на непривилегированного пользователя
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q -O - http://127.0.0.1:${SERVER_PORT:-8181}/actuator/health | grep -q UP

# Оптимизации JVM для контейнера
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-jar", \
    "app.jar"]