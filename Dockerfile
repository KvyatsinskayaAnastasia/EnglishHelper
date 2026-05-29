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

# Собираем приложение (чистка не нужна, т.к. используем чистый контейнер)
RUN ./mvnw package -DskipTests

# Финальный stage - только с JRE и собранным jar
FROM eclipse-temurin:17-jre-alpine

# Создаем непривилегированного пользователя для безопасности
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Копируем только jar файл из builder stage
COPY --from=builder /app/target/english-*.jar app.jar

# Переключаемся на непривилегированного пользователя
USER appuser

# Оптимизации JVM для контейнера
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-jar", \
    "app.jar"]