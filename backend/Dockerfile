FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -DskipTests
COPY src src/
RUN ./mvnw package -DskipTests
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

FROM eclipse-temurin:17-jre-jammy
ARG UID=10001
RUN adduser --disabled-password --gecos "" --home "/nonexistent" --shell "/sbin/nologin" --no-create-home --uid "${UID}" appuser \
    && mkdir -p /app/uploads \
    && chown -R appuser:appuser /app
WORKDIR /app
COPY --from=builder --chown=appuser:appuser /build/target/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /build/target/extracted/application/ ./
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
