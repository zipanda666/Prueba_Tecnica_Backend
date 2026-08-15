# ---- Etapa 1: build ----
# Compila el proyecto dentro del contenedor, asi nadie necesita Maven instalado en su maquina
# para levantar el backend via Docker (solo para desarrollo local con mvn spring-boot:run).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar solo el pom.xml primero y descargar dependencias en una capa separada:
# si despues solo cambias codigo fuente (no el pom), Docker reusa esta capa cacheada
# y no vuelve a descargar todo Maven Central en cada build.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Etapa 2: runtime ----
# Imagen final solo con JRE (no el JDK completo ni Maven) - mucho mas liviana.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
