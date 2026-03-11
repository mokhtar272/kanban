# ── Étape 1 : Build du JAR ─────────────────────────────────────
# Contexte de build = spring/  (passer spring/ dans la commande docker build)
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copier les fichiers Gradle en premier (cache Docker)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

# Résoudre les dépendances
RUN gradle dependencies --no-daemon || true

# Copier les sources et compiler
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ── Étape 2 : Image finale légère ──────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
