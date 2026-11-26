# Etapa 1: Compilacao com JDK 25 dentro do Docker
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# Copia primeiro os arquivos do Maven para aproveitar o cache de dependencias
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline
# Copia o codigo fonte e compila o .jar
COPY src ./src
RUN ./mvnw clean package -DskipTests
# Etapa 2: Imagem final leve apenas com JRE 25
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]