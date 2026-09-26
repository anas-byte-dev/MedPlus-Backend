# ==========================================
# Stage 1: Build Application with Maven & Java 25
# ==========================================
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# Cache Maven dependencies in Docker layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build production fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Production JRE Runtime
# ==========================================
FROM eclipse-temurin:25-jre
WORKDIR /app

# Non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy packaged JAR from builder stage
COPY --from=builder --chown=spring:spring /build/target/medpulse-backend-*.jar app.jar

# Render assigns port dynamically via $PORT (defaults to 8080)
ENV PORT=8080
EXPOSE 8080

# Memory tuning tailored for Render Free Tier (512MB RAM cap) with strict IPv4 stack
# -Xms128m -Xmx384m ensures JVM leaves room for OS/metaspace without OOMKilled
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.net.preferIPv4Stack=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
