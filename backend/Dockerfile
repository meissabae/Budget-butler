# ============================================
# STAGE 1: Build the app with Maven
# We use a temporary "builder" image that has Maven + JDK installed, just to compile
# the project into a runnable .jar file. This image gets thrown away afterward -
# it never ships in the final image, keeping things small.
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy just the pom.xml first and download dependencies - Docker caches this layer,
# so if you only change your Java code (not pom.xml), rebuilds are much faster.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy the actual source code and build the jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================
# STAGE 2: Run the app with just a JRE (no Maven, no JDK - smaller & more secure)
# ============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy ONLY the built jar from the "build" stage above - none of the source code
# or Maven cache ends up in this final image.
COPY --from=build /app/target/*.jar app.jar

# Render (and most hosts) tell the container which port to listen on via $PORT.
# Spring Boot's server.port property can be overridden by an environment variable,
# so we pass it through here instead of hardcoding 8080.
ENV PORT=8080
EXPOSE 8080

# -Xmx400m caps JVM memory to fit inside free-tier hosting limits (usually 512MB total).
ENTRYPOINT ["sh", "-c", "java -Xmx400m -Dserver.port=${PORT} -jar app.jar"]
