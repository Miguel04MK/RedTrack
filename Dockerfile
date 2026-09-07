# ============================================================
#  Build
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Las dependencias en su propia capa: si solo cambia el codigo,
# Docker no vuelve a descargar medio Maven Central.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ============================================================
#  Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# No arrancar como root.
RUN addgroup -S redtrack && adduser -S redtrack -G redtrack

COPY --from=build /build/target/*.jar app.jar
RUN chown redtrack:redtrack /app/app.jar
USER redtrack

EXPOSE 8080

# Usa el endpoint de Actuator: si la app no responde, el contenedor se marca
# como no sano y compose lo reinicia.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
